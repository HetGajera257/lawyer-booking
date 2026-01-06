import React from 'react';

const CaseList = ({ cases, onSelectCase, showAssignButton, userType, onAssign }) => {
    if (!cases || cases.length === 0) {
        return (
            <div className="no-cases-message">
                <p>No cases found.</p>
            </div>
        );
    }

    const getStatusColor = (status) => {
        switch (status?.toLowerCase()) {
            case 'open':
            case 'new':
                return { bg: '#e3f2fd', color: '#1976d2' };
            case 'in-progress':
                return { bg: '#fff3e0', color: '#f57c00' };
            case 'solved':
            case 'closed':
                return { bg: '#e8f5e9', color: '#2e7d32' };
            default:
                return { bg: '#f3e5f5', color: '#7b1fa2' };
        }
    };

    return (
        <div className="cases-list">
            {cases.map((caseItem) => {
                const style = getStatusColor(caseItem.caseStatus);
                return (
                    <div
                        key={caseItem.id}
                        className="case-card"
                        onClick={() => onSelectCase && onSelectCase(caseItem)}
                        style={{
                            padding: '20px',
                            marginBottom: '20px',
                            background: '#fff',
                            borderRadius: '8px',
                            boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
                            cursor: onSelectCase ? 'pointer' : 'default',
                            border: '1px solid #eee',
                            transition: 'transform 0.2s',
                        }}
                        onMouseOver={(e) => {
                            if (onSelectCase) e.currentTarget.style.transform = 'translateY(-2px)';
                        }}
                        onMouseOut={(e) => {
                            if (onSelectCase) e.currentTarget.style.transform = 'translateY(0)';
                        }}
                    >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                            <h3 style={{ margin: '0 0 10px 0', color: '#2c3e50' }}>{caseItem.caseTitle}</h3>
                            <span style={{
                                padding: '4px 12px',
                                borderRadius: '20px',
                                fontSize: '0.85rem',
                                fontWeight: '600',
                                backgroundColor: style.bg,
                                color: style.color
                            }}>
                                {caseItem.caseStatus}
                            </span>
                        </div>

                        {/* Description Preview */}
                        {caseItem.description && (
                            <p style={{ color: '#666', fontSize: '0.95rem', lineHeight: '1.5', margin: '10px 0' }}>
                                {caseItem.description.length > 200
                                    ? `${caseItem.description.substring(0, 200)}...`
                                    : caseItem.description}
                            </p>
                        )}

                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '15px' }}>
                            <small style={{ color: '#999' }}>
                                Created: {new Date(caseItem.createdAt).toLocaleDateString()}
                            </small>

                            {showAssignButton && (
                                <button
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        onAssign(caseItem.id);
                                    }}
                                    style={{
                                        padding: '8px 16px',
                                        backgroundColor: '#3498db',
                                        color: 'white',
                                        border: 'none',
                                        borderRadius: '4px',
                                        cursor: 'pointer',
                                        fontWeight: '500'
                                    }}
                                >
                                    Connect / Accept
                                </button>
                            )}

                            {!showAssignButton && (
                                <span style={{ color: '#3498db', fontSize: '0.9rem', fontWeight: '500' }}>
                                    View Details &rarr;
                                </span>
                            )}
                        </div>
                    </div>
                );
            })}
        </div>
    );
};

export default CaseList;
