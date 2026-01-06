import React, { useState, useEffect, useRef } from 'react';
import { toast } from 'react-toastify';

import { messagesApi } from '../utils/api';

function UserCaseMessages({ caseId, userId, lawyerId }) {
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef(null);

  useEffect(() => {
    if (caseId) {
      fetchMessages();
      // Poll for new messages every 5 seconds
      const interval = setInterval(fetchMessages, 5000);
      return () => clearInterval(interval);
    }
  }, [caseId]);

  useEffect(() => {
    // Scroll to bottom when new messages arrive
    scrollToBottom();
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const fetchMessages = async () => {
    if (!caseId) return;

    try {
      const response = await messagesApi.getByCase(caseId);
      // Axios stores data in response.data
      const data = response.data;
      setMessages(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Error fetching messages:', err);
    }
  };

  const sendMessage = async () => {
    if (!newMessage.trim() || !caseId || !userId || !lawyerId) {
      toast.warning('Please enter a message');
      return;
    }

    setLoading(true);
    try {
      const messageData = {
        caseId: caseId,
        senderId: userId,
        senderType: 'user',
        receiverId: lawyerId,
        receiverType: 'lawyer',
        messageText: newMessage.trim()
      };

      await messagesApi.send(messageData);

      setNewMessage('');
      await fetchMessages();
      toast.success('Message sent successfully');

    } catch (err) {
      console.error('Error sending message:', err);
      // Axios error handling
      const errorMessage = err.response?.data?.message || 'Failed to send message';
      if (err.response?.status === 401) {
        toast.error('Unauthorized. Please login again.');
      } else {
        toast.error(errorMessage);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  return (
    <div style={{
      border: '1px solid #ddd',
      borderRadius: '8px',
      padding: '20px',
      backgroundColor: '#f9f9f9',
      maxHeight: '500px',
      display: 'flex',
      flexDirection: 'column'
    }}>
      <h3 style={{ marginTop: 0, marginBottom: '15px' }}>Messages</h3>

      <div style={{
        flex: 1,
        overflowY: 'auto',
        marginBottom: '15px',
        padding: '10px',
        backgroundColor: 'white',
        borderRadius: '4px',
        minHeight: '200px',
        maxHeight: '300px'
      }}>
        {messages.length === 0 ? (
          <p style={{ color: '#666', textAlign: 'center', padding: '20px' }}>
            No messages yet. Start the conversation!
          </p>
        ) : (
          messages.map((msg) => (
            <div
              key={msg.id}
              style={{
                marginBottom: '15px',
                padding: '10px',
                backgroundColor: msg.senderType === 'user' ? '#e3f2fd' : '#fff3e0',
                borderRadius: '8px',
                textAlign: msg.senderType === 'user' ? 'right' : 'left',
                border: msg.senderType === 'user' ? '1px solid #2196f3' : '1px solid #ff9800'
              }}
            >
              <div style={{ fontWeight: 'bold', marginBottom: '5px' }}>
                {msg.senderType === 'user' ? 'You' : 'Lawyer'}:
              </div>
              <div style={{ marginBottom: '5px' }}>{msg.messageText}</div>
              <div style={{ fontSize: '12px', color: '#666' }}>
                {new Date(msg.createdAt).toLocaleString()}
                {msg.isRead && msg.senderType === 'user' && (
                  <span style={{ marginLeft: '5px', color: '#4caf50' }}>✓ Read</span>
                )}
              </div>
            </div>
          ))
        )}
        <div ref={messagesEndRef} />
      </div>

      <div style={{ display: 'flex', gap: '10px' }}>
        <input
          type="text"
          value={newMessage}
          onChange={(e) => setNewMessage(e.target.value)}
          onKeyPress={handleKeyPress}
          placeholder="Type your message..."
          disabled={loading || !lawyerId}
          style={{
            flex: 1,
            padding: '10px',
            borderRadius: '4px',
            border: '1px solid #ddd',
            fontSize: '14px'
          }}
        />
        <button
          onClick={sendMessage}
          disabled={loading || !newMessage.trim() || !lawyerId}
          style={{
            padding: '10px 20px',
            backgroundColor: loading ? '#ccc' : '#3498db',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: loading ? 'not-allowed' : 'pointer',
            fontSize: '14px',
            fontWeight: 'bold'
          }}
        >
          {loading ? 'Sending...' : 'Send'}
        </button>
      </div>

      {!lawyerId && (
        <p style={{ color: '#f44336', fontSize: '12px', marginTop: '10px' }}>
          No lawyer assigned to this case yet. Messages can be sent once a lawyer is assigned.
        </p>
      )}
    </div>
  );
}

export default UserCaseMessages;
