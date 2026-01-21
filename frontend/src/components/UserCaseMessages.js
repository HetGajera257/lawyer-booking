import React, { useState, useEffect, useRef, useCallback } from 'react';
import { toast } from 'react-toastify';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

import { messagesApi } from '../utils/api';

function UserCaseMessages({ caseId, userId, userType, lawyerId, clientUserId, onCaseUpdate }) {
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [connected, setConnected] = useState(false);
  const messagesEndRef = useRef(null);
  const stompClientRef = useRef(null);

  const fetchMessages = useCallback(async () => {
    if (!caseId) return;
    try {
      const response = await messagesApi.getByCase(caseId);
      setMessages(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      console.error('Error fetching messages:', err);
    }
  }, [caseId]);

  const connectWebSocket = useCallback(() => {
    // Determine base URL for WebSocket dynamically
    const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:';
    const host = window.location.hostname;
    const port = '8080'; // Backend port
    const socketUrl = `${protocol}//${host}:${port}/ws`;

    console.log('Attempting WebSocket connection to:', socketUrl);

    const socket = new SockJS(socketUrl);
    const client = new Client({
      webSocketFactory: () => socket,
      connectionTimeout: 10000,
      connectHeaders: {
        // Authorization header removed as per requirement to decouple WS from JWT
      },
      debug: (str) => {
        if (str.includes('ERROR')) console.error('STOMP: ' + str);
        else console.log('STOMP: ' + str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    client.onConnect = (frame) => {
      console.log('Connected to WebSocket: ' + frame);
      setConnected(true);
      client.subscribe(`/topic/case/${caseId}`, (message) => {
        try {
          const receivedData = JSON.parse(message.body);

          // Check if it's a chat message (has messageText) or a case update (has caseTitle or solution)
          if (receivedData.messageText) {
            setMessages((prev) => {
              if (prev.some((m) => m.id === receivedData.id)) return prev;
              return [...prev, receivedData];
            });
          } else if (receivedData.caseTitle !== undefined || receivedData.solution !== undefined) {
            console.log('Received real-time case update:', receivedData);
            if (onCaseUpdate) {
              onCaseUpdate(receivedData);
            }
          }
        } catch (e) {
          console.error('Error parsing broadcast message:', e);
        }
      });
    };

    client.onStompError = (frame) => {
      console.error('STOMP protocol error:', frame.headers['message']);
      setConnected(false);
    };

    client.onWebSocketClose = () => {
      console.warn('WebSocket connection closed');
      setConnected(false);
    };

    try {
      client.activate();
      stompClientRef.current = client;
    } catch (err) {
      console.error('Failed to activate STOMP client:', err);
    }
  }, [caseId]);

  useEffect(() => {
    if (caseId) {
      fetchMessages();
      connectWebSocket();
    }
    return () => disconnectWebSocket();
  }, [caseId, fetchMessages, connectWebSocket]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };



  const disconnectWebSocket = () => {
    if (stompClientRef.current) {
      stompClientRef.current.deactivate();
    }
  };



  const sendMessage = async () => {
    if (!newMessage.trim() || !caseId || !userId || !userType) {
      toast.warning('Please enter a message');
      return;
    }

    // Determine receiver based on current user type
    const isLawyer = userType === 'lawyer';
    const receiverId = isLawyer ? clientUserId : lawyerId;
    const receiverType = isLawyer ? 'user' : 'lawyer';

    if (!receiverId) {
      toast.error('Waiting for other party to join...');
      return;
    }

    const messageData = {
      caseId: caseId,
      senderId: userId,
      senderType: userType,
      receiverId: receiverId,
      receiverType: receiverType,
      messageText: newMessage.trim()
    };

    if (stompClientRef.current && connected) {
      stompClientRef.current.publish({
        destination: '/app/chat.send',
        body: JSON.stringify(messageData),
      });
      setNewMessage('');
    } else {
      // Fallback to REST if WS is down
      try {
        await messagesApi.send(messageData);
        setNewMessage('');
      } catch (err) {
        toast.error('Failed to send message');
      }
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
      borderRadius: '12px',
      padding: '24px',
      backgroundColor: '#fcfcfc',
      maxHeight: '600px',
      display: 'flex',
      flexDirection: 'column',
      boxShadow: '0 4px 12px rgba(0,0,0,0.05)'
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <h3 style={{ margin: 0, fontSize: '1.2rem', color: '#333' }}>Case Discussion</h3>
        <span style={{
          fontSize: '12px',
          fontWeight: '600',
          color: connected ? '#2ecc71' : '#e74c3c',
          display: 'flex',
          alignItems: 'center',
          gap: '6px',
          padding: '4px 10px',
          borderRadius: '20px',
          backgroundColor: connected ? '#eafaf1' : '#fdedec'
        }}>
          <span style={{
            width: '8px',
            height: '8px',
            borderRadius: '50%',
            backgroundColor: connected ? '#2ecc71' : '#e74c3c'
          }} />
          {connected ? 'Realtime Connected' : 'Disconnected'}
        </span>
      </div>

      <div style={{
        flex: 1,
        overflowY: 'auto',
        marginBottom: '20px',
        padding: '15px',
        backgroundColor: '#fff',
        borderRadius: '8px',
        minHeight: '300px',
        maxHeight: '400px',
        border: '1px solid #f0f0f0'
      }}>
        {messages.length === 0 ? (
          <div style={{ color: '#888', textAlign: 'center', padding: '40px 20px' }}>
            <p style={{ margin: 0 }}>No messages in this case yet.</p>
            <p style={{ fontSize: '0.9rem', marginTop: '5px' }}>Start the conversation by sending a message below.</p>
          </div>
        ) : (
          messages.map((msg) => {
            // Logic to determine if I am the sender
            const isMe = String(msg.senderId) === String(userId) && msg.senderType === userType;

            // Determine label for the other party
            let otherPartyLabel = 'Other';
            if (msg.senderType === 'user') otherPartyLabel = 'Client';
            if (msg.senderType === 'lawyer') otherPartyLabel = 'Lawyer';

            return (
              <div
                key={msg.id || Math.random()}
                style={{
                  marginBottom: '16px',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: isMe ? 'flex-end' : 'flex-start',
                }}
              >
                <div style={{
                  display: 'flex',
                  flexDirection: 'column',
                  maxWidth: '85%',
                }}>
                  <div style={{
                    fontWeight: '600',
                    fontSize: '12px',
                    marginBottom: '4px',
                    color: isMe ? '#2980b9' : '#7f8c8d',
                    textAlign: isMe ? 'right' : 'left'
                  }}>
                    {isMe ? 'You' : otherPartyLabel}
                  </div>
                  <div style={{
                    padding: '12px 16px',
                    backgroundColor: isMe ? '#e3f2fd' : '#f5f5f5',
                    color: '#333',
                    borderRadius: '16px',
                    borderTopRightRadius: isMe ? '4px' : '16px',
                    borderTopLeftRadius: isMe ? '16px' : '4px',
                    boxShadow: '0 1px 1px rgba(0,0,0,0.05)',
                    lineHeight: '1.5',
                    fontSize: '0.95rem'
                  }}>
                    {msg.messageText}
                  </div>
                  <div style={{
                    fontSize: '10px',
                    color: '#bdc3c7',
                    marginTop: '4px',
                    textAlign: isMe ? 'right' : 'left'
                  }}>
                    {msg.createdAt ? new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Sending...'}
                  </div>
                </div>
              </div>
            );
          })
        )}
        <div ref={messagesEndRef} />
      </div>

      <div style={{ display: 'flex', gap: '12px' }}>
        <input
          type="text"
          value={newMessage}
          onChange={(e) => setNewMessage(e.target.value)}
          onKeyDown={handleKeyPress}
          placeholder="Write your message..."
          disabled={!lawyerId && userType === 'user'}
          style={{
            flex: 1,
            padding: '14px 20px',
            borderRadius: '28px',
            border: '1px solid #e0e0e0',
            fontSize: '15px',
            outline: 'none',
            backgroundColor: '#fff',
            transition: 'border-color 0.2s',
          }}
          onFocus={(e) => e.target.style.borderColor = '#3498db'}
          onBlur={(e) => e.target.style.borderColor = '#e0e0e0'}
        />
        <button
          onClick={sendMessage}
          disabled={!newMessage.trim() || (!lawyerId && userType === 'user')}
          style={{
            width: '50px',
            height: '50px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            backgroundColor: !newMessage.trim() ? '#f0f0f0' : '#3498db',
            color: 'white',
            border: 'none',
            borderRadius: '50%',
            cursor: !newMessage.trim() ? 'default' : 'pointer',
            fontSize: '20px',
            transition: 'all 0.2s',
            boxShadow: !newMessage.trim() ? 'none' : '0 4px 8px rgba(52, 152, 219, 0.3)'
          }}
        >
          <span style={{ transform: 'rotate(-45deg)', marginLeft: '4px' }}>➤</span>
        </button>
      </div>

      {!lawyerId && userType === 'user' && (
        <p style={{ color: '#e74c3c', fontSize: '13px', marginTop: '15px', textAlign: 'center', backgroundColor: '#fdedec', padding: '8px', borderRadius: '4px' }}>
          <strong>Waiting for a lawyer:</strong> You can start chatting once a lawyer accepts your case.
        </p>
      )}
    </div>
  );
}

export default UserCaseMessages;
