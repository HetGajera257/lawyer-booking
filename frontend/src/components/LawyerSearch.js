import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { lawyersApi, casesApi } from '../utils/api';
import { toast } from 'react-toastify';
import './LawyerSearch.css';

const LawyerSearch = ({ caseId, onSelectSuccess, onBack }) => {
    const navigate = useNavigate();
    const [lawyers, setLawyers] = useState([]);
    const [loading, setLoading] = useState(false);
    const [filters, setFilters] = useState({
        specialization: '',
        minRating: 0,
        minExperience: 0,
        name: ''
    });
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    const fetchLawyers = useCallback(async () => {
        setLoading(true);
        try {
            const params = {
                ...filters,
                page,
                size: 10,
                sort: 'rating,desc'
            };
            // Clean up empty filters
            Object.keys(params).forEach(key => {
                if (params[key] === '' || params[key] === 0) delete params[key];
            });

            const response = await lawyersApi.search(params);
            setLawyers(response.data.lawyers || []);
            setTotalPages(response.data.totalPages || 0);
        } catch (err) {
            console.error('Error fetching lawyers:', err);
            const errorMsg = err.response?.data?.message || err.response?.data?.error || 'Failed to load lawyers. Please try again.';
            toast.error(errorMsg);
        } finally {
            setLoading(false);
        }
    }, [filters, page]);

    useEffect(() => {
        const timer = setTimeout(() => {
            fetchLawyers();
        }, 500); // Debounce search
        return () => clearTimeout(timer);
    }, [fetchLawyers]);

    const handleFilterChange = (e) => {
        const { name, value } = e.target;
        setFilters(prev => ({ ...prev, [name]: value }));
        setPage(0); // Reset to first page on filter change
    };

    const handleAssign = async (lawyerId) => {
        if (!caseId) {
            toast.error('No case selected for assignment');
            return;
        }

        try {
            await casesApi.assignLawyer(caseId, lawyerId);
            toast.success('Lawyer assigned successfully!');
            if (onSelectSuccess) onSelectSuccess(lawyerId);
        } catch (err) {
            console.error('Error assigning lawyer:', err);
            toast.error(err.response?.data?.message || 'Failed to assign lawyer.');
        }
    };

    return (
        <div className="lawyer-search-container">
            <aside className="search-sidebar">
                <h3>Filter Lawyers</h3>

                <div className="filter-group">
                    <label>Lawyer Name</label>
                    <input
                        type="text"
                        name="name"
                        value={filters.name}
                        onChange={handleFilterChange}
                        placeholder="Search by name..."
                        className="filter-control"
                    />
                </div>

                <div className="filter-group">
                    <label>Specialization</label>
                    <select
                        name="specialization"
                        value={filters.specialization}
                        onChange={handleFilterChange}
                        className="filter-control"
                    >
                        <option value="">All Specializations</option>
                        <option value="Family Law">Family Law</option>
                        <option value="Criminal Law">Criminal Law</option>
                        <option value="Civil Law">Civil Law</option>
                        <option value="Corporate Law">Corporate Law</option>
                        <option value="Real Estate">Real Estate</option>
                    </select>
                </div>

                <div className="filter-group">
                    <label>Minimum Rating</label>
                    <div className="range-wrap">
                        <input
                            type="range"
                            name="minRating"
                            min="0"
                            max="5"
                            step="0.5"
                            value={filters.minRating}
                            onChange={handleFilterChange}
                            className="filter-control"
                        />
                        <span className="range-val">{filters.minRating} ★ and above</span>
                    </div>
                </div>

                <div className="filter-group">
                    <label>Min Experience (Years)</label>
                    <div className="range-wrap">
                        <input
                            type="range"
                            name="minExperience"
                            min="0"
                            max="30"
                            value={filters.minExperience}
                            onChange={handleFilterChange}
                            className="filter-control"
                        />
                        <span className="range-val">{filters.minExperience}+ years</span>
                    </div>
                </div>

                {onBack && (
                    <button onClick={onBack} className="btn-view" style={{ width: '100%', marginTop: '20px' }}>
                        &larr; Back to Case
                    </button>
                )}
            </aside>

            <main className="results-section">
                {onBack && (
                    <div style={{ marginBottom: '20px' }}>
                        <button
                            onClick={onBack}
                            className="btn-view"
                            style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: '8px',
                                padding: '10px 20px',
                                fontSize: '14px'
                            }}
                        >
                            <span style={{ fontSize: '18px' }}>←</span> Back to Case Details
                        </button>
                    </div>
                )}
                <div className="results-header">
                    <h2>Available Legal Experts</h2>
                    <span className="results-count">Showing {lawyers.length} lawyers</span>
                </div>

                {loading ? (
                    <div className="no-results">Searching for experts...</div>
                ) : lawyers.length > 0 ? (
                    <div className="results-grid">
                        {lawyers.map(lawyer => (
                            <div key={lawyer.id} className="lawyer-card">
                                <div className="lawyer-card-header">
                                    <div className="lawyer-avatar">
                                        {lawyer.fullName.charAt(0)}
                                    </div>
                                    <div className="lawyer-info">
                                        <h4>{lawyer.fullName}</h4>
                                        <span className="lawyer-spec">{lawyer.specialization || 'General Practice'}</span>
                                    </div>
                                </div>
                                <div className="lawyer-card-body">
                                    <div className="stat-row">
                                        <span className="stat-label">Rating</span>
                                        <span className="stat-value lawyer-rating">{lawyer.rating || 'N/A'} ★</span>
                                    </div>
                                    <div className="stat-row">
                                        <span className="stat-label">Experience</span>
                                        <span className="stat-value">{lawyer.yearsOfExperience || 0} Years</span>
                                    </div>
                                    <div className="stat-row">
                                        <span className="stat-label">Cases Won</span>
                                        <span className="stat-value">{lawyer.completedCasesCount || 0}</span>
                                    </div>
                                    <div className="stat-row">
                                        <span className="stat-label">Availability</span>
                                        <span className="stat-value" style={{ color: '#27ae60' }}>Available</span>
                                    </div>
                                </div>
                                <div className="lawyer-card-footer">
                                    <button
                                        className="btn-view"
                                        onClick={() => {
                                            // Open in new tab with full URL
                                            window.open(`${window.location.origin}/lawyer/${lawyer.id}`, '_blank');
                                        }}
                                    >
                                        View Profile
                                    </button>
                                    <button
                                        className="btn-select"
                                        onClick={() => handleAssign(lawyer.id)}
                                    >
                                        Select Expert
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                ) : (
                    <div className="no-results">
                        <h3>No lawyers matching your criteria</h3>
                        <p>Try adjusting your filters to see more results.</p>
                    </div>
                )}

                {totalPages > 1 && (
                    <div className="pagination" style={{ display: 'flex', gap: '10px', justifyContent: 'center', marginTop: '30px' }}>
                        <button disabled={page === 0} onClick={() => setPage(page - 1)} className="btn-view">Previous</button>
                        <span style={{ alignSelf: 'center' }}>Page {page + 1} of {totalPages}</span>
                        <button disabled={page === totalPages - 1} onClick={() => setPage(page + 1)} className="btn-view">Next</button>
                    </div>
                )}
            </main>
        </div>
    );
};

export default LawyerSearch;
