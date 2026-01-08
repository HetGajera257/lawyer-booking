import React, { useState, useEffect, useCallback } from 'react';
import UserCaseMessages from './UserCaseMessages';
import LawyerProfile from './LawyerProfile';
import AppointmentsList from './AppointmentsList';
import Booking from './Booking';
import LawyerSearch from './LawyerSearch';
import { casesApi } from '../utils/api';
import { toast } from 'react-toastify';

const CaseDetailView = ({ caseId, userType, userId, lawyerId, onBack, setShowLawyerSearch }) => {
    const [caseData, setCaseData] = useState(null);
    const [solution, setSolution] = useState('');
    const [loading, setLoading] = useState(true);
    const [savingSolution, setSavingSolution] = useState(false);
    const [showBookingModal, setShowBookingModal] = useState(false);
    const [error, setError] = useState('');

    const fetchCaseDetails = useCallback(async () => {
        setLoading(true);
        try {
            // If we don't have the full object, fetch it
            const response = await casesApi.getById(caseId);
            if (response.data) {
                setCaseData(response.data);
                setSolution(response.data.solution || '');
            }
        } catch (err) {
            console.error('Error fetching case details:', err);
            setError('Failed to load case details.');
        } finally {
            setLoading(false);
        }
    }, [caseId]);

    useEffect(() => {
        fetchCaseDetails();
    }, [fetchCaseDetails]);

    const handleSaveSolution = async () => {
        if (!solution.trim()) {
            toast.warning('Solution cannot be empty');
            return;
        }

        setSavingSolution(true);
        try {
            await casesApi.updateSolution(caseId, solution);
            const updatedCase = { ...caseData, solution, caseStatus: 'solved' };
            setCaseData(updatedCase);

            // Also update status to solved/closed if not already
            if (caseData.caseStatus !== 'solved') {
                await casesApi.updateStatus(caseId, 'solved');
            }

            toast.success('Solution saved successfully!');
        } catch (err) {
            console.error('Error saving solution:', err);
            toast.error('Failed to save solution.');
        } finally {
            setSavingSolution(false);
        }
    };

    if (loading) return <div className="p-4">Loading case details...</div>;
    if (error) return <div className="p-4 text-red-500">{error} <button onClick={onBack}>Go Back</button></div>;
    if (!caseData) return null;

    const isLawyer = userType === 'lawyer';

    return (
        <div className="case-detail-container" style={{ padding: '20px', background: '#fff', borderRadius: '8px' }}>
            <button
                onClick={onBack}
                style={{
                    marginBottom: '20px',
                    background: 'transparent',
                    border: 'none',
                    color: '#3498db',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    fontSize: '1rem'
                }}
            >
                &larr; Back to List
            </button>

            <div style={{ borderBottom: '1px solid #eee', paddingBottom: '20px', marginBottom: '20px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <h2 style={{ margin: 0 }}>{caseData.caseTitle}</h2>
                    <span style={{
                        padding: '6px 12px',
                        borderRadius: '20px',
                        background: caseData.caseStatus === 'open' ? '#e3f2fd' : '#e8f5e9',
                        color: caseData.caseStatus === 'open' ? '#1976d2' : '#2e7d32',
                        fontWeight: 'bold'
                    }}>
                        {caseData.caseStatus.toUpperCase()}
                    </span>
                </div>
                {!isLawyer && !caseData.lawyerId && (
                    <div style={{ marginTop: '15px', padding: '15px', background: '#fff9c4', borderRadius: '8px', border: '1px solid #fff176', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div>
                            <p style={{ margin: 0, fontWeight: 'bold', color: '#f57f17' }}>Case Unassigned</p>
                            <p style={{ margin: '5px 0 0 0', fontSize: '0.9rem', color: '#616161' }}>Select a legal expert to start your consultation.</p>
                        </div>
                        <button
                            onClick={() => setShowLawyerSearch(true)}
                            style={{
                                padding: '10px 20px',
                                backgroundColor: '#fbc02d',
                                color: '#000',
                                border: 'none',
                                borderRadius: '6px',
                                cursor: 'pointer',
                                fontWeight: 'bold',
                                boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
                            }}
                        >
                            Find Lawyer →
                        </button>
                    </div>
                )}
                <p style={{ color: '#666', marginTop: '10px' }}>
                    <strong>Case ID:</strong> {caseData.id} |
                    <strong> Created:</strong> {new Date(caseData.createdAt).toLocaleString()}
                </p>
            </div>

            <div className="case-description" style={{ marginBottom: '30px', padding: '15px', background: '#f8f9fa', borderRadius: '8px' }}>
                <h3 style={{ marginTop: 0, fontSize: '1.1rem' }}>Description</h3>
                <p style={{ lineHeight: '1.6', color: '#2c3e50' }}>{caseData.description}</p>
            </div>

            <div className="case-solution-section" style={{ marginBottom: '30px', padding: '20px', backgroundColor: '#fff', borderRadius: '12px', border: '1px solid #e0e0e0', boxShadow: '0 4px 6px rgba(0,0,0,0.02)' }}>
                <h3 style={{ fontSize: '1.2rem', borderLeft: '5px solid #27ae60', paddingLeft: '15px', marginBottom: '20px', color: '#2c3e50' }}>Legal Solution / Advice</h3>
                {isLawyer ? (
                    <div className="editor-section">
                        <textarea
                            value={solution}
                            onChange={(e) => setSolution(e.target.value)}
                            placeholder="Write your professional legal advice here..."
                            style={{
                                width: '100%',
                                minHeight: '150px',
                                padding: '15px',
                                borderRadius: '10px',
                                border: '1px solid #d1d1d1',
                                marginBottom: '15px',
                                fontFamily: 'inherit',
                                fontSize: '1rem',
                                lineHeight: '1.6',
                                resize: 'vertical'
                            }}
                        />
                        <button
                            onClick={handleSaveSolution}
                            disabled={savingSolution}
                            style={{
                                padding: '10px 24px',
                                backgroundColor: '#27ae60',
                                color: 'white',
                                border: 'none',
                                borderRadius: '8px',
                                cursor: savingSolution ? 'not-allowed' : 'pointer',
                                fontWeight: 'bold',
                                fontSize: '1rem',
                                transition: 'all 0.3s'
                            }}
                        >
                            {savingSolution ? 'Saving...' : 'Submit Professional Advice'}
                        </button>
                    </div>
                ) : (
                    <div style={{
                        padding: '20px',
                        background: solution ? '#f0fff4' : '#fffaf0',
                        borderRadius: '10px',
                        border: solution ? '1px solid #c6f6d5' : '1px solid #feebc8',
                        minHeight: '80px',
                        display: 'flex',
                        alignItems: 'center'
                    }}>
                        {solution ? (
                            <p style={{ whiteSpace: 'pre-wrap', margin: 0, fontSize: '1.05rem', color: '#2d3748', lineHeight: '1.7' }}>{solution}</p>
                        ) : !caseData.lawyerId ? (
                            <div style={{ textAlign: 'center', width: '100%', color: '#718096' }}>
                                <i style={{ fontSize: '1.1rem' }}>No lawyer assigned yet. Use the "Find Lawyer" button above to get started.</i>
                            </div>
                        ) : (
                            <div style={{ textAlign: 'center', width: '100%', color: '#c05621' }}>
                                <i style={{ fontSize: '1.1rem' }}>Your lawyer is currently reviewing your case. The solution will appear here soon.</i>
                            </div>
                        )}
                    </div>
                )}
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '24px', marginBottom: '30px' }}>
                <div className="case-messages">
                    <UserCaseMessages
                        caseId={caseId}
                        userId={userId}
                        userType={userType}
                        lawyerId={caseData.lawyerId}
                        clientUserId={caseData.userId}
                        onCaseUpdate={(updatedCase) => {
                            setCaseData(updatedCase);
                            if (updatedCase.solution !== undefined) {
                                setSolution(updatedCase.solution);
                            }
                        }}
                    />
                </div>

                <div className="case-sidebar">
                    {!isLawyer && caseData.lawyerId && (
                        <div className="lawyer-expert-profile">
                            <h3 style={{ fontSize: '1.1rem', marginBottom: '15px' }}>Assigned Lawyer</h3>
                            <LawyerProfile lawyerId={caseData.lawyerId} />
                        </div>
                    )}
                </div>
            </div>

            <div className="case-appointments" style={{ marginTop: '40px', borderTop: '2px solid #f0f0f0', paddingTop: '30px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                    <h3 style={{ fontSize: '1.2rem', margin: 0 }}>Case Appointments</h3>
                    {!isLawyer && caseData.lawyerId && (
                        <button
                            onClick={() => setShowBookingModal(true)}
                            style={{
                                padding: '8px 16px',
                                backgroundColor: '#3498db',
                                color: 'white',
                                border: 'none',
                                borderRadius: '5px',
                                cursor: 'pointer',
                                fontWeight: 'bold'
                            }}
                        >
                            + Book Follow-up
                        </button>
                    )}
                </div>

                <AppointmentsList
                    userId={userId}
                    userType={userType}
                    caseId={caseId}
                />
            </div>

            {showBookingModal && (
                <div className="modal-overlay" style={{
                    position: 'fixed',
                    top: 0,
                    left: 0,
                    right: 0,
                    bottom: 0,
                    backgroundColor: 'rgba(0,0,0,0.5)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    zIndex: 2000,
                    padding: '20px'
                }}>
                    <div className="modal-content" style={{
                        backgroundColor: 'white',
                        borderRadius: '12px',
                        width: '100%',
                        maxWidth: '600px',
                        maxHeight: '90vh',
                        overflowY: 'auto',
                        position: 'relative',
                        padding: '20px',
                        boxShadow: '0 10px 25px rgba(0,0,0,0.2)'
                    }}>
                        <button
                            onClick={() => setShowBookingModal(false)}
                            style={{
                                position: 'absolute',
                                top: '15px',
                                right: '15px',
                                border: 'none',
                                background: 'none',
                                fontSize: '24px',
                                cursor: 'pointer',
                                color: '#666'
                            }}
                        >
                            ×
                        </button>
                        <div style={{ marginTop: '10px' }}>
                            <h2 style={{ marginBottom: '20px', color: '#2c3e50', fontSize: '1.4rem' }}>Book Follow-up</h2>
                            <Booking
                                userId={userId}
                                appointment={{
                                    lawyerId: caseData.lawyerId,
                                    caseId: caseId
                                }}
                                onBookingSuccess={() => {
                                    setShowBookingModal(false);
                                    window.location.reload();
                                }}
                            />
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

const CaseDetail = (props) => {
    const [showLawyerSearchInternal, setShowLawyerSearchInternal] = useState(false);

    if (showLawyerSearchInternal) {
        return (
            <div className="case-detail-container">
                <LawyerSearch
                    caseId={props.caseId}
                    onBack={() => setShowLawyerSearchInternal(false)}
                    onSelectSuccess={() => {
                        setShowLawyerSearchInternal(false);
                        window.location.reload(); // Simple way to refresh case state
                    }}
                />
            </div>
        );
    }

    return <CaseDetailView {...props} setShowLawyerSearch={setShowLawyerSearchInternal} />;
};

export default CaseDetail;
