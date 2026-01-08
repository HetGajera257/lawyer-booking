import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { removeToken } from '../utils/auth';
import AppointmentsList from './AppointmentsList';
import CaseList from './CaseList';
import CaseDetail from './CaseDetail';
import { casesApi, audioApi } from '../utils/api';
import './Dashboard.css';
import { toast } from 'react-toastify';

function LawyerDashboard() {
    const [records, setRecords] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [playingRecordId, setPlayingRecordId] = useState(null);
    const [currentAudioUrl, setCurrentAudioUrl] = useState(null);
    const [selectedLanguage, setSelectedLanguage] = useState({}); // { recordId: 'en' | 'gu' }
    const [activeTab, setActiveTab] = useState('audio'); // 'audio', 'appointments', or 'cases'
    const [lawyerId, setLawyerId] = useState(null);
    const [cases, setCases] = useState([]);
    const [unassignedCases, setUnassignedCases] = useState([]);
    const [casesLoading, setCasesLoading] = useState(false);
    const [selectedCase, setSelectedCase] = useState(null);
    const [creatingCaseId, setCreatingCaseId] = useState(null); // Track which audio is being processed
    const audioRef = useRef(null);
    const navigate = useNavigate();

    useEffect(() => {
        // Check if lawyer is logged in
        const userType = localStorage.getItem('userType');
        if (userType !== 'lawyer') {
            navigate('/lawyer-login');
        } else {
            // Get lawyerId from localStorage
            const storedLawyerId = localStorage.getItem('lawyerId');
            if (storedLawyerId) {
                setLawyerId(parseInt(storedLawyerId));
            }
        }
    }, [navigate]);

    const fetchCases = useCallback(async () => {
        if (!lawyerId) return;
        setCasesLoading(true);
        try {
            // Fetch unassigned cases
            const unassignedResponse = await casesApi.getUnassigned();
            setUnassignedCases(Array.isArray(unassignedResponse.data) ? unassignedResponse.data : []);

            // Fetch cases assigned to this lawyer
            const assignedResponse = await casesApi.getByLawyer(lawyerId);
            setCases(Array.isArray(assignedResponse.data) ? assignedResponse.data : []);
        } catch (err) {
            console.error('Error fetching cases:', err);
            toast.error('Failed to load cases');
        } finally {
            setCasesLoading(false);
        }
    }, [lawyerId]);

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
            fetchCases(); // Refresh lists
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

            // 3. Refresh and switch view
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

    return (
        <div className="dashboard-container">
            <div className="dashboard-header">
                <h1>Lawyer Dashboard</h1>
                <div className="header-actions">
                    <span className="username">Welcome, {localStorage.getItem('username') || 'Lawyer'}</span>
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
                >
                    📋 Cases
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
                                    <h2 style={{ borderBottom: '2px solid #3498db', paddingBottom: '10px' }}>Unassigned Cases</h2>
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
                                    <h2 style={{ borderBottom: '2px solid #27ae60', paddingBottom: '10px' }}>My Cases</h2>
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
            </div>
        </div>
    );
}

export default LawyerDashboard;
