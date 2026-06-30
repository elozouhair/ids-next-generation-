import axios from 'axios';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const BASE_URL = '/api';

export const api = {
  getDashboard: () => axios.get(`${BASE_URL}/dashboard`).then(r => r.data),
  getAlerts: (page = 0, size = 50) =>
    axios.get(`${BASE_URL}/alerts`, { params: { page, size } }).then(r => r.data),
  getAlertsByType: (type) =>
    axios.get(`${BASE_URL}/alerts/type/${type}`).then(r => r.data),
  getHighSeverity: (minutes = 60) =>
    axios.get(`${BASE_URL}/alerts/high-severity`, { params: { minutes } }).then(r => r.data),
  getAlertCount: () =>
    axios.get(`${BASE_URL}/alerts/count`).then(r => r.data),
  getGeoLocations: () =>
    axios.get(`${BASE_URL}/geo/recent`).then(r => r.data),
  getAttackTimeline: (minutes = 60) =>
    axios.get(`${BASE_URL}/alerts/attack-timeline`, { params: { minutes } }).then(r => r.data),
};

export function connectWebSocket(onAlert, onDashboard, onGeo) {
  const client = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
  });

  client.onConnect = () => {
    client.subscribe('/topic/alerts', (msg) => {
      if (onAlert) onAlert(JSON.parse(msg.body));
    });
    client.subscribe('/topic/dashboard', (msg) => {
      if (onDashboard) onDashboard(JSON.parse(msg.body));
    });
    if (onGeo) {
      client.subscribe('/topic/geo', (msg) => {
        onGeo(JSON.parse(msg.body));
      });
    }
  };

  client.activate();
  return client;
}
