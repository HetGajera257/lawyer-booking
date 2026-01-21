import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { removeToken } from '../utils/auth';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import AppointmentsList from './AppointmentsList';
import CaseList from './CaseList';
import CaseDetail from './CaseDetail';
import LawyerProfile from './LawyerProfile';
import { casesApi, audioApi, lawyersApi } from '../utils/api';
import './Dashboard.css';
import { toast } from 'react-toastify';

function LawyerDashboard() {
    const [records, setRecords] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [playingRecordId, setPlayingRecordId] = useState(null);
    const [currentAudioUrl, setCurrentAudioUrl] = useState(null);
    const [selectedLanguage, setSelectedLanguage] = useState({}); // { recordId: 'en' | 'gu' }
    const [activeTab, setActiveTab] = useState('audio'); // 'audio', 'appointments', 'cases', or 'profile'
    const [lawyerId, setLawyerId] = useState(null);
    const [cases, setCases] = useState([]);
    const [unassignedCases, setUnassignedCases] = useState([]);
    const [casesLoading, setCasesLoading] = useState(false);
    const [selectedCase, setSelectedCase] = useState(null);
    const [creatingCaseId, setCreatingCaseId] = useState(null);
    const [lawyerProfile, setLawyerProfile] = useState(null);
    const lawyerProfileRef = useRef(null); // Ref to avoid stale closures in WS
    const [pendingRequests, setPendingRequests] = useState([]); // Real-time requests
    const audioRef = useRef(null);
    const navigate = useNavigate();

    const fetchLawyerProfile = useCallback(async (id) => {
        try {
            const response = await lawyersApi.getProfile(id || lawyerId);
            setLawyerProfile(response.data);
            lawyerProfileRef.current = response.data; // Update ref for WS
            console.log('Lawyer profile loaded:', response.data);
        } catch (err) {
            console.error('Error fetching lawyer profile:', err);
        }
    }, [lawyerId]);

    const fetchCases = useCallback(async () => {
        if (!lawyerId) return;
        setCasesLoading(true);
        console.log(`DEBUG: Dashboard fetching cases for Lawyer ID: ${lawyerId}`);
        try {
            // Fetch unassigned cases - Use recommended if possible
            const unassignedResponse = await casesApi.getRecommended(lawyerId);
            setUnassignedCases(Array.isArray(unassignedResponse.data) ? unassignedResponse.data : []);

            // Fetch cases assigned to this lawyer specifically
            const assignedResponse = await casesApi.getByLawyer(lawyerId);
            const myCases = Array.isArray(assignedResponse.data) ? assignedResponse.data : [];
            console.log(`DEBUG: Found ${myCases.length} assigned cases for Lawyer ${lawyerId}`);
            setCases(myCases);
        } catch (err) {
            console.error('Error fetching cases:', err);
            if (err.response?.status === 401) {
                toast.error('Session expired or unauthorized. Please login again.');
                handleLogout();
            } else {
                toast.error('Failed to load cases');
            }
        } finally {
            setCasesLoading(false);
        }
    }, [lawyerId]);

    useEffect(() => {
        // Check if lawyer is logged in
        const userType = localStorage.getItem('userType');
        if (!userType) {
            navigate('/lawyer-login');
            return;
        }

        if (userType === 'user') {
            navigate('/user-dashboard');
            return;
        }

        if (userType !== 'lawyer') {
            navigate('/lawyer-login');
            return;
        }

        // Get lawyerId from localStorage
        const storedLawyerId = localStorage.getItem('lawyerId');
        if (storedLawyerId) {
            const id = parseInt(storedLawyerId);
            setLawyerId(id);
            fetchLawyerProfile(id);
        }
    }, [navigate, fetchLawyerProfile]);


    // WebSocket for notifications
    useEffect(() => {
        if (!lawyerId) {
            console.log('WS SKIP: No lawyerId available for connection');
            return;
        }

        console.log('WS INIT: Starting connection for lawyer:', lawyerId);
        const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:';
        const host = window.location.hostname;
        const port = '8080';
        const socketUrl = `${protocol}//${host}:${port}/ws`;

        const socket = new SockJS(socketUrl);
        const client = new Client({
            webSocketFactory: () => socket,
            reconnectDelay: 5000,
            onConnect: () => {
                console.log('WS CONNECT: Connected to request service at /topic/lawyer/requests');
                client.subscribe('/topic/lawyer/requests', (message) => {
                    const receivedPayload = JSON.parse(message.body);
                    const currentProfile = lawyerProfileRef.current;

                    console.log('WS RECEIVED: Raw payload:', receivedPayload);

                    // Filter by specialization if profile is available
                    const specsPlural = (currentProfile?.specializations || "").toLowerCase();
                    const specsSingular = (currentProfile?.specialization || "").toLowerCase();
                    const category = (receivedPayload.category || receivedPayload.caseCategory || "").toLowerCase();

                    console.log(`WS FILTER: Matching "${category}" against specs: ["${specsSingular}", "${specsPlural}"]`);

                    const isMatch = !currentProfile ||
                        specsPlural.includes(category) ||
                        specsSingular.includes(category) ||
                        category === "" ||
                        (specsPlural === "" && specsSingular === "");

                    console.log(`WS RESULT: isMatch = ${isMatch}`);

                    if (isMatch) {
                        setPendingRequests(prev => {
                            if (prev.some(r => r.caseId === receivedPayload.caseId)) return prev;
                            return [receivedPayload, ...prev];
                        });

                        toast.info(
                            <div>
                                <strong>🆕 New Case Request: {receivedPayload.title}</strong>
                                <p style={{ fontSize: '0.85rem', margin: '5px 0', color: '#666' }}>
                                    Category: {receivedPayload.category || 'General'}
                                </p>
                                <p style={{ fontSize: '0.75rem', color: '#888' }}>
                                    {receivedPayload.description && receivedPayload.description.length > 80
                                        ? receivedPayload.description.substring(0, 80) + '...'
                                        : receivedPayload.description}
                                </p>
                            </div>,
                            {
                                position: "top-right",
                                autoClose: 30000,
                                onClick: () => {
                                    setActiveTab('cases');
                                    fetchCases();
                                }
                            }
                        );
                        // Refresh unassigned cases silently
                        fetchCases();
                    } else {
                        console.log(`WS FILTER SKIP: Request for "${category}" does not match lawyer specializations.`);
                    }
                });

                // Subscribe to global lawyer updates (e.g. case assigned)
                client.subscribe('/topic/lawyer/updates', (message) => {
                    const update = JSON.parse(message.body);
                    console.log('WS UPDATE RECEIVED:', update);
                    if (update.type === 'CASE_ASSIGNED') {
                        const caseId = update.caseId;
                        // 1. Remove from local unassigned/pending lists immediately
                        setUnassignedCases(prev => prev.filter(c => c.id !== caseId));
                        setPendingRequests(prev => prev.filter(r => r.caseId !== caseId));

                        // 2. Refresh My Cases and Records (to update button states)
                        fetchCases();
                        fetchRecords();
                    }
                });
            },
            onStompError: (frame) => {
                console.error('WS ERROR:', frame.headers['message']);
            }
        });

        client.activate();

        return () => {
            console.log('WS CLEANUP: Deactivating client');
            client.deactivate();
        };
    }, [lawyerId, fetchCases]);

    const fetchRecords = useCallback(async () => {
        setLoading(true);
        setError('');

        try {
            const response = await audioApi.getAll();
            setRecords(Array.isArray(response.data) ? response.data : []);
        } catch (err) {
            setError('Error fetching records: ' + err.message);
            console.error('Error fetching records:', err);
            setRecords([]);
        } finally {
            setLoading(false);
        }
    }, []);

    // Fetch initial data based on tab
    useEffect(() => {
        if (activeTab === 'audio') fetchRecords();
        if (activeTab === 'cases' && lawyerId) fetchCases();
    }, [activeTab, lawyerId, fetchRecords, fetchCases]);

    const connectToCase = useCallback(async (caseId) => {
        if (!lawyerId) return;
        try {
            await casesApi.assignLawyer(caseId, lawyerId);
            toast.success('Successfully connected to case!');

            // Immediate local cleanup to prevent double-click or stale display
            setUnassignedCases(prev => prev.filter(c => c.id !== caseId));
            setPendingRequests(prev => prev.filter(r => r.caseId !== caseId));

            fetchCases(); // Refresh My Cases
            fetchRecords(); // Refresh Audio button states
        } catch (err) {
            console.error('Error connecting to case:', err);
            toast.error('Error connecting to case');
        }
    }, [lawyerId, fetchCases]);

    const createCaseFromAudio = useCallback(async (record) => {
        if (!lawyerId) return;

        // Check for userId in the record
        const targetUserId = record.userId || record.user_id;

        if (!targetUserId) {
            toast.error("Cannot create case: Audio record is missing User ID (Backend update required).");
            return;
        }

        setCreatingCaseId(record.id);
        try {
            // 1. Create Case
            const description = (record.maskedEnglishText || record.maskedGujaratiText || "Audio Record Case") + "\n\n(Generated from Audio Record #" + record.id + ")";
            const caseData = {
                userId: targetUserId,
                caseTitle: `Case from Audio #${record.id}`,
                caseType: 'General',
                description: description
            };

            const createResponse = await casesApi.create(caseData);
            const newCase = createResponse.data || createResponse;

            if (!newCase || !newCase.id) throw new Error("Failed to create case");

            // 2. Assign Lawyer
            await casesApi.assignLawyer(newCase.id, lawyerId);

            toast.success("Case created and assigned successfully!");
            const caseId = newCase.id;

            // 3. Clear from pools immediately
            setUnassignedCases(prev => prev.filter(c => c.id !== caseId));
            setPendingRequests(prev => prev.filter(r => r.caseId !== caseId));

            // 4. Refresh and switch view
            await fetchRecords();
            await fetchCases();
            setActiveTab('cases');
            setSelectedCase(newCase);

        } catch (err) {
            console.error("Error creating case from audio:", err);
            toast.error("Failed to create case: " + (err.response?.data?.message || err.message));
        } finally {
            setCreatingCaseId(null);
        }
    }, [lawyerId, fetchRecords, fetchCases]);



    const handleLogout = () => {
        // Stop any playing audio before logout
        if (audioRef.current) {
            audioRef.current.pause();
            audioRef.current = null;
        }
        if (currentAudioUrl) {
            URL.revokeObjectURL(currentAudioUrl);
            setCurrentAudioUrl(null);
        }
        setPlayingRecordId(null);
        removeToken();
        navigate('/lawyer-login');
    };

    // Cleanup audio on unmount
    useEffect(() => {
        return () => {
            if (audioRef.current) {
                audioRef.current.pause();
                audioRef.current = null;
            }
        };
    }, []);

    const playAudio = (audioData, recordId) => {
        if (!audioData) {
            setError('No audio data available');
            return;
        }

        // Toggle logic
        if (playingRecordId === recordId && audioRef.current) {
            if (!audioRef.current.paused) {
                audioRef.current.pause();
                setPlayingRecordId(null);
                return;
            }
            audioRef.current.play().catch(console.error);
            return;
        }

        if (audioRef.current) {
            audioRef.current.pause();
        }
        if (currentAudioUrl) {
            URL.revokeObjectURL(currentAudioUrl);
        }

        try {
            let audioBlob;
            if (typeof audioData === 'string') {
                const base64String = audioData.replace(/^data:audio\/\w+;base64,/, '');
                const binaryString = atob(base64String);
                const bytes = new Uint8Array(binaryString.length);
                for (let i = 0; i < binaryString.length; i++) bytes[i] = binaryString.charCodeAt(i);
                audioBlob = new Blob([bytes], { type: 'audio/mpeg' });
            } else {
                audioBlob = new Blob([new Uint8Array(audioData)], { type: 'audio/mpeg' });
            }

            const url = URL.createObjectURL(audioBlob);
            setCurrentAudioUrl(url);
            const audio = new Audio(url);
            audioRef.current = audio;
            setPlayingRecordId(recordId);

            audio.play().catch(e => setError('Playback error: ' + e.message));
            audio.onended = () => setPlayingRecordId(null);

        } catch (err) {
            console.error('Error playing audio:', err);
            setError('Error playing audio');
        }
    };

    // Listen for storage events to handle multi-tab session changes
    useEffect(() => {
        const handleStorageChange = (e) => {
            if (e.key === 'token' || e.key === 'userType') {
                const newUserType = localStorage.getItem('userType');
                if (newUserType === 'user') {
                    navigate('/user-dashboard');
                } else if (!newUserType) {
                    navigate('/lawyer-login');
                }
            }
        };

        window.addEventListener('storage', handleStorageChange);
        return () => window.removeEventListener('storage', handleStorageChange);
    }, [navigate]);

    const checkUserType = localStorage.getItem('userType');
    if (!checkUserType || checkUserType !== 'lawyer') return null;

    return (
        <div className="dashboard-container">
            <div className="dashboard-header">
                <h1>Lawyer Dashboard</h1>
                <div className="header-actions">
                    <span className="username">Welcome, {localStorage.getItem('username') || 'Lawyer'}</span>
                    <button
                        onClick={() => setActiveTab('profile')}
                        className="profile-button"
                        style={{
                            padding: '8px 16px',
                            backgroundColor: '#f0f4f8',
                            color: '#3498db',
                            border: '1px solid #3498db',
                            borderRadius: '8px',
                            cursor: 'pointer',
                            fontWeight: '600',
                            marginRight: '10px'
                        }}
                    >
                        👤 My Profile
                    </button>
                    <button onClick={handleLogout} className="logout-button">Logout</button>
                </div>
            </div>

            <div className="tab-navigation">
                <button
                    className={activeTab === 'audio' ? 'tab-button active' : 'tab-button'}
                    onClick={() => { setActiveTab('audio'); setSelectedCase(null); }}
                >
                    🎤 Audio Records
                </button>
                <button
                    className={activeTab === 'appointments' ? 'tab-button active' : 'tab-button'}
                    onClick={() => { setActiveTab('appointments'); setSelectedCase(null); }}
                >
                    📅 Appointments
                </button>
                <button
                    className={activeTab === 'cases' ? 'tab-button active' : 'tab-button'}
                    onClick={() => { setActiveTab('cases'); setSelectedCase(null); }}
                    style={{ position: 'relative' }}
                >
                    📋 Cases
                    {pendingRequests.length > 0 && (
                        <span style={{
                            position: 'absolute',
                            top: '-8px',
                            right: '-8px',
                            backgroundColor: '#e74c3c',
                            color: 'white',
                            borderRadius: '50%',
                            padding: '2px 6px',
                            fontSize: '10px',
                            fontWeight: 'bold',
                            boxShadow: '0 2px 4px rgba(0,0,0,0.2)'
                        }}>
                            {pendingRequests.length}
                        </span>
                    )}
                </button>
                <button
                    className={activeTab === 'profile' ? 'tab-button active' : 'tab-button'}
                    onClick={() => { setActiveTab('profile'); setSelectedCase(null); }}
                >
                    👤 My Profile
                </button>
            </div>

            <div className="dashboard-content">
                {activeTab === 'audio' && (
                    <div className="records-section">
                        <div className="section-header">
                            <h2>Client Audio Records</h2>
                            <button onClick={fetchRecords} className="refresh-button" disabled={loading}>
                                {loading ? 'Loading...' : 'Refresh'}
                            </button>
                        </div>

                        {error && <div className="error-message">{error}</div>}

                        {loading ? (
                            <div className="loading-message">Loading records...</div>
                        ) : records.length === 0 ? (
                            <div className="empty-state">
                                <p>No records found.</p>
                            </div>
                        ) : (
                            <div className="records-grid">
                                {records.map((record) => (
                                    <div key={record.id} className="record-card">
                                        <div className="record-header">
                                            <h3>Record ID: {record.id}</h3>
                                            <span className="record-language">{record.language || 'N/A'}</span>
                                        </div>

                                        <div className="record-content">
                                            <div className="record-field">
                                                <h4>Language:</h4>
                                                <div style={{ display: 'flex', gap: '10px', marginBottom: '15px' }}>
                                                    {['en', 'gu'].map(lang => (
                                                        <button
                                                            key={lang}
                                                            onClick={() => setSelectedLanguage({ ...selectedLanguage, [record.id]: lang })}
                                                            style={{
                                                                padding: '5px 10px',
                                                                background: (selectedLanguage[record.id] || 'en') === lang ? '#3498db' : '#ecf0f1',
                                                                color: (selectedLanguage[record.id] || 'en') === lang ? 'white' : 'black',
                                                                borderRadius: '4px', border: 'none', cursor: 'pointer'
                                                            }}
                                                        >
                                                            {lang === 'en' ? 'English' : 'Gujarati'}
                                                        </button>
                                                    ))}
                                                </div>
                                            </div>

                                            <div className="record-field">
                                                <h4>Text:</h4>
                                                <div className="text-content">
                                                    {selectedLanguage[record.id] === 'gu'
                                                        ? (record.maskedGujaratiText || 'N/A')
                                                        : (record.maskedEnglishText || 'N/A')}
                                                </div>
                                            </div>

                                            <div className="record-field">
                                                <h4>Audio & Actions:</h4>
                                                <div style={{ display: 'flex', alignItems: 'center', gap: '15px', flexWrap: 'wrap' }}>
                                                    {(() => {
                                                        const isGujarati = selectedLanguage[record.id] === 'gu';
                                                        const audioData = isGujarati
                                                            ? (record.maskedGujaratiAudioBase64 || record.maskedGujaratiAudio)
                                                            : (record.maskedTextAudioBase64 || record.maskedTextAudio);

                                                        return audioData ? (
                                                            <button
                                                                onClick={() => playAudio(audioData, record.id)}
                                                                className="play-audio-button"
                                                            >
                                                                {playingRecordId === record.id ? '⏸ Pause' : '▶ Play'}
                                                            </button>
                                                        ) : <span style={{ color: '#999' }}>No Audio</span>;
                                                    })()}

                                                    {/* Select / Create Case Button - Synced with Connect/Accept */}
                                                    <button
                                                        className="create-case-button"
                                                        style={{
                                                            padding: '8px 16px',
                                                            backgroundColor: record.lawyerId ? '#95a5a6' : record.caseId ? '#3498db' : '#2ecc71',
                                                            color: 'white',
                                                            border: 'none',
                                                            borderRadius: '4px',
                                                            cursor: record.lawyerId ? 'not-allowed' : 'pointer',
                                                            opacity: creatingCaseId === record.id ? 0.7 : 1,
                                                            fontWeight: 'bold'
                                                        }}
                                                        disabled={!!record.lawyerId || creatingCaseId === record.id}
                                                        onClick={() => record.caseId ? connectToCase(record.caseId) : createCaseFromAudio(record)}
                                                    >
                                                        {creatingCaseId === record.id ? 'Processing...' :
                                                            record.lawyerId ? 'Case Already Assigned' :
                                                                record.caseId ? 'Connect / Accept Case' : 'Create & Accept Case'}
                                                    </button>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                )}

                {activeTab === 'appointments' && (
                    <div className="appointments-tab-content">
                        {lawyerId ? (
                            <AppointmentsList userId={lawyerId} userType="lawyer" />
                        ) : (
                            <div className="error-message">Lawyer ID not found.</div>
                        )}
                    </div>
                )}

                {activeTab === 'cases' && (
                    <div className="cases-tab-content">
                        {/* existing cases logic... */}
                        {selectedCase ? (
                            <CaseDetail
                                caseId={selectedCase.id}
                                userType="lawyer"
                                userId={lawyerId}
                                onBack={() => { setSelectedCase(null); fetchCases(); }}
                            />
                        ) : (
                            <div style={{ display: 'flex', gap: '20px', height: '100%' }}>
                                <div style={{ flex: '1', overflowY: 'auto' }}>
                                    <h2 style={{ borderBottom: '2px solid #3498db', paddingBottom: '10px' }}>
                                        📥 Incoming Case Requests
                                    </h2>
                                    {pendingRequests.length > 0 && (
                                        <div className="pending-requests-banner" style={{ marginBottom: '20px' }}>
                                            <CaseList
                                                cases={pendingRequests.map(r => ({
                                                    ...r,
                                                    id: r.caseId,
                                                    caseTitle: r.title,
                                                    caseCategory: r.category,
                                                    caseStatus: 'NEW REQUEST'
                                                }))}
                                                showAssignButton={true}
                                                onAssign={(id) => {
                                                    connectToCase(id);
                                                    setPendingRequests(prev => prev.filter(r => r.caseId !== id));
                                                }}
                                                userType="lawyer"
                                            />
                                        </div>
                                    )}
                                    <h2 style={{ borderBottom: '2px solid #95a5a6', paddingBottom: '10px', marginTop: '20px', fontSize: '1.2rem' }}>
                                        All Unassigned Cases
                                    </h2>
                                    {casesLoading && <p>Loading...</p>}
                                    {!casesLoading && (
                                        <CaseList
                                            cases={unassignedCases}
                                            showAssignButton={true}
                                            onAssign={connectToCase}
                                            userType="lawyer"
                                        />
                                    )}
                                </div>

                                <div style={{ flex: '1', overflowY: 'auto', borderLeft: '1px solid #ddd', paddingLeft: '20px' }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '2px solid #27ae60', paddingBottom: '10px' }}>
                                        <h2 style={{ margin: 0 }}>My Cases</h2>
                                        <button
                                            onClick={fetchCases}
                                            style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '1.2rem', color: '#27ae60' }}
                                            title="Refresh My Cases"
                                        >
                                            🔄
                                        </button>
                                    </div>
                                    <p style={{ fontSize: '0.8rem', color: '#666', marginTop: '5px' }}>
                                        Showing cases assigned to <strong>{lawyerProfile?.fullName || 'you'}</strong>
                                    </p>
                                    {!casesLoading && (
                                        <CaseList
                                            cases={cases}
                                            onSelectCase={setSelectedCase}
                                            userType="lawyer"
                                        />
                                    )}
                                </div>
                            </div>
                        )}
                    </div>
                )}

                {activeTab === 'profile' && (
                    <div className="profile-tab-content">
                        <LawyerProfile
                            lawyerId={lawyerId}
                            onUpdate={(updatedProfile) => {
                                setLawyerProfile(updatedProfile);
                            }}
                        />
                    </div>
                )}
            </div>
        </div>
    );
}

export default LawyerDashboard;
