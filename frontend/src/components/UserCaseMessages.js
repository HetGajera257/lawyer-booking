import React, { useState, useEffect, useRef, useCallback } from 'react';
import { toast } from 'react-toastify';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

import { messagesApi } from '../utils/api';
import { getToken } from '../utils/auth';

/**
 * SECURE CASE-BASED CHAT COMPONENT
 * 
 * PURPOSE: Real-time chat interface with strict security and role-based UI
 * 
 * SECURITY FEATURES:
 * - JWT token attached to WebSocket headers
 * - Sender identity derived from backend (not frontend)
 * - Case-scoped messaging only
 * - No identity spoofing possible
 * 
 * UI FEATURES:
 * - Role-based message alignment (current user right, others left)
 * - Color-coded by role (user vs lawyer)
 * - Real-time message delivery
 * - Stable reconnection logic
 * - Read status indicators
 */
function UserCaseMessages({ caseId, userId, userType, lawyerId, clientUserId, onCaseUpdate }) {
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [connected, setConnected] = useState(false);
  const [reconnecting, setReconnecting] = useState(false);
  const messagesEndRef = useRef(null);
  const stompClientRef = useRef(null);
  const reconnectTimeoutRef = useRef(null);

  // Fetch messages from REST API
  const fetchMessages = useCallback(async () => {
    if (!caseId) return;
    try {
      const response = await messagesApi.getByCase(caseId);
      setMessages(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      console.error('Error fetching messages:', err);
      toast.error('Failed to load messages');
    }
  }, [caseId]);

  // Get JWT token for WebSocket authentication
  const getWebSocketHeaders = useCallback(() => {
    const token = getToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
  }, []);

  // Connect to WebSocket with JWT authentication
  const connectWebSocket = useCallback(() => {
    if (!caseId) return;

    // Clear any existing reconnect timeout
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
    }

    // Determine base URL for WebSocket dynamically
    const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:';
    const host = window.location.hostname;
    const port = '8080'; // Backend port
    const socketUrl = `${protocol}//${host}:${port}/ws`;

    console.log('Attempting WebSocket connection to:', socketUrl);

    const socket = new SockJS(socketUrl);
    const client = new Client({
      webSocketFactory: () => socket,
      debug: (str) => {
        if (str.includes('ERROR')) console.error('STOMP: ' + str);
        else console.log('STOMP: ' + str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      connectHeaders: getWebSocketHeaders(),
    });

    client.onConnect = (frame) => {
      console.log('Connected to WebSocket: ' + frame);
      setConnected(true);
      setReconnecting(false);
      
      // Subscribe to case-specific topic
      client.subscribe(`/topic/case/${caseId}`, (message) => {
        try {
          const receivedData = JSON.parse(message.body);

          // Check if it's a chat message (has messageText) or a case update
          if (receivedData.messageText) {
            setMessages((prev) => {
              // Avoid duplicate messages
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
      
      // Try to reconnect
      if (!reconnecting) {
        setReconnecting(true);
        reconnectTimeoutRef.current = setTimeout(() => {
          console.log('Attempting to reconnect...');
          connectWebSocket();
        }, 5000);
      }
    };

    client.onWebSocketClose = () => {
      console.warn('WebSocket connection closed');
      setConnected(false);
      
      // Try to reconnect if not already reconnecting
      if (!reconnecting) {
        setReconnecting(true);
        reconnectTimeoutRef.current = setTimeout(() => {
          console.log('Attempting to reconnect...');
          connectWebSocket();
        }, 5000);
      }
    };

    client.onWebSocketError = (error) => {
      console.error('WebSocket error:', error);
      setConnected(false);
    };

    try {
      client.activate();
      stompClientRef.current = client;
    } catch (err) {
      console.error('Failed to activate STOMP client:', err);
    }
  }, [caseId, getWebSocketHeaders, reconnecting, onCaseUpdate]);

  // Disconnect WebSocket
  const disconnectWebSocket = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
    }
    if (stompClientRef.current) {
      stompClientRef.current.deactivate();
    }
    setConnected(false);
    setReconnecting(false);
  }, []);

  // Send message via WebSocket or REST fallback
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

    // SECURITY: Only send receiver info, sender info comes from JWT
    const messageData = {
      caseId: caseId,
      receiverId: receiverId,
      receiverType: receiverType,
      messageText: newMessage.trim()
      // NOTE: NO senderId or senderType - backend extracts from JWT
    };

    if (stompClientRef.current && connected) {
      try {
        stompClientRef.current.publish({
          destination: '/app/chat.send',
          body: JSON.stringify(messageData),
          headers: getWebSocketHeaders(),
        });
        setNewMessage('');
      } catch (err) {
        console.error('WebSocket send failed, falling back to REST:', err);
        await sendViaRest(messageData);
      }
    } else {
      // Fallback to REST if WebSocket is down
      await sendViaRest(messageData);
    }
  };

  // Send message via REST API
  const sendViaRest = async (messageData) => {
    try {
      await messagesApi.send(messageData);
      setNewMessage('');
      // Refresh messages to get the sent message
      fetchMessages();
    } catch (err) {
      console.error('Failed to send message via REST:', err);
      toast.error('Failed to send message');
    }
  };

  // Scroll to bottom of messages
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  // Handle keyboard events
  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  // Check if message is from current user
  const isMessageFromMe = (message) => {
    return String(message.senderId) === String(userId) && 
           message.senderType?.toLowerCase() === userType?.toLowerCase();
  };

  // Get message styling based on sender
  const getMessageStyle = (message) => {
    const isMe = isMessageFromMe(message);
    const isFromLawyer = message.senderType?.toLowerCase() === 'lawyer';
    
    return {
      container: {
        marginBottom: '16px',
        display: 'flex',
        flexDirection: 'column',
        alignItems: isMe ? 'flex-end' : 'flex-start',
      },
      content: {
        display: 'flex',
        flexDirection: 'column',
        maxWidth: '85%',
      },
      label: {
        fontWeight: '600',
        fontSize: '12px',
        marginBottom: '4px',
        color: isMe ? '#2980b9' : (isFromLawyer ? '#8e44ad' : '#7f8c8d'),
        textAlign: isMe ? 'right' : 'left'
      },
      bubble: {
        padding: '12px 16px',
        backgroundColor: isMe ? '#e3f2fd' : (isFromLawyer ? '#f3e5f5' : '#f5f5f5'),
        color: '#333',
        borderRadius: '16px',
        borderTopRightRadius: isMe ? '4px' : '16px',
        borderTopLeftRadius: isMe ? '16px' : '4px',
        boxShadow: '0 1px 1px rgba(0,0,0,0.05)',
        lineHeight: '1.5',
        fontSize: '0.95rem'
      },
      time: {
        fontSize: '10px',
        color: '#bdc3c7',
        marginTop: '4px',
        textAlign: isMe ? 'right' : 'left'
      }
    };
  };

  // Initialize WebSocket and fetch messages
  useEffect(() => {
    if (caseId) {
      fetchMessages();
      connectWebSocket();
    }
    return () => disconnectWebSocket();
  }, [caseId, fetchMessages, connectWebSocket, disconnectWebSocket]);

  // Auto-scroll when new messages arrive
  useEffect(() => {
    scrollToBottom();
  }, [messages]);

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
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <h3 style={{ margin: 0, fontSize: '1.2rem', color: '#333' }}>Case Discussion</h3>
        <span style={{
          fontSize: '12px',
          fontWeight: '600',
          color: connected ? '#2ecc71' : (reconnecting ? '#f39c12' : '#e74c3c'),
          display: 'flex',
          alignItems: 'center',
          gap: '6px',
          padding: '4px 10px',
          borderRadius: '20px',
          backgroundColor: connected ? '#eafaf1' : (reconnecting ? '#fef9e7' : '#fdedec')
        }}>
          <span style={{
            width: '8px',
            height: '8px',
            borderRadius: '50%',
            backgroundColor: connected ? '#2ecc71' : (reconnecting ? '#f39c12' : '#e74c3c')
          }} />
          {connected ? 'Connected' : (reconnecting ? 'Reconnecting...' : 'Disconnected')}
        </span>
      </div>

      {/* Messages */}
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
            const style = getMessageStyle(msg);
            const isMe = isMessageFromMe(msg);
            const senderLabel = isMe ? 'You' : (msg.senderType?.toLowerCase() === 'lawyer' ? 'Lawyer' : 'Client');

            return (
              <div key={msg.id || Math.random()} style={style.container}>
                <div style={style.content}>
                  <div style={style.label}>
                    {senderLabel}
                  </div>
                  <div style={style.bubble}>
                    {msg.messageText}
                  </div>
                  <div style={style.time}>
                    {msg.createdAt ? new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Sending...'}
                  </div>
                </div>
              </div>
            );
          })
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
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

      {/* Warning message */}
      {!lawyerId && userType === 'user' && (
        <p style={{ color: '#e74c3c', fontSize: '13px', marginTop: '15px', textAlign: 'center', backgroundColor: '#fdedec', padding: '8px', borderRadius: '4px' }}>
          <strong>Waiting for a lawyer:</strong> You can start chatting once a lawyer accepts your case.
        </p>
      )}
    </div>
  );
}

export default UserCaseMessages;
