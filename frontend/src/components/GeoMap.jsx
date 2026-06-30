import React, { useEffect, useRef, useState, useCallback } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { api, connectWebSocket } from '../services/api';
import { AlertTriangle } from 'lucide-react';

const COLORS = {
  BENIGN: '#22c55e',
  'DoS Hulk': '#ef4444',
  DDoS: '#dc2626',
  PortScan: '#f59e0b',
  'Brute Force': '#f97316',
  Bot: '#a855f7',
  'Web Attack - XSS': '#ec4899',
  Infiltration: '#6366f1',
  Attack: '#ef4444',
  Normal: '#22c55e',
  high: '#ef4444',
  medium: '#f59e0b',
  low: '#3b82f6',
};

const SEVERITY_COLORS = { high: '#dc2626', medium: '#f59e0b', low: '#3b82f6', info: '#64748b' };

function createPulsingIcon(color) {
  return L.divIcon({
    className: 'pulsing-marker',
    html: `<div style="
      width: 20px; height: 20px; background: ${color}; border-radius: 50%;
      border: 2px solid white; box-shadow: 0 0 0 rgba(0,0,0,0);
      animation: pulseMarker 1.5s ease-in-out infinite;
    "><div style="
      width: 40px; height: 40px; background: ${color}40; border-radius: 50%;
      position: absolute; top: -10px; left: -10px;
      animation: pulseRing 1.5s ease-in-out infinite;
    "></div></div>`,
    iconSize: [20, 20],
    iconAnchor: [10, 10],
  });
}

const notificationStyle = {
  notificationBar: {
    display: 'flex', alignItems: 'center', gap: 10, padding: '8px 14px',
    borderRadius: 8, marginBottom: 10, fontSize: 12,
    animation: 'slideDown 0.3s ease-out',
    transition: 'all 0.3s',
  },
};

export default function GeoMap() {
  const mapRef = useRef(null);
  const mapInstance = useRef(null);
  const markersRef = useRef(null);
  const [locations, setLocations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [notifications, setNotifications] = useState([]);
  const [newAlertCount, setNewAlertCount] = useState(0);
  const [showPanel, setShowPanel] = useState(false);
  const prevLocationsRef = useRef(0);

  const addNotification = useCallback((alert) => {
    const id = Date.now() + Math.random();
    const newNotif = {
      id,
      attackType: alert.attackType || 'Unknown',
      srcIp: alert.srcIp || '--',
      severity: alert.severity || 'info',
      timestamp: new Date().toLocaleTimeString(),
      latitude: alert.latitude,
      longitude: alert.longitude,
    };
    setNotifications(prev => [newNotif, ...prev].slice(0, 50));
    setTimeout(() => {
      setNotifications(prev => prev.filter(n => n.id !== id));
    }, 8000);
  }, []);

  useEffect(() => {
    api.getGeoLocations().then(data => {
      const slice = data.slice(0, 500);
      setLocations(slice);
      prevLocationsRef.current = slice.length;
      setLoading(false);
    }).catch(() => setLoading(false));

    const ws = connectWebSocket(null, null, (newLocs) => {
      if (!Array.isArray(newLocs) || newLocs.length === 0) return;
      setNewAlertCount(prev => prev + newLocs.length);
      setLocations(prev => [...newLocs, ...prev].slice(0, 500));
      newLocs.forEach(loc => addNotification(loc));
    });

    return () => { if (ws) ws.deactivate(); };
  }, [addNotification]);

  useEffect(() => {
    if (mapInstance.current || !mapRef.current) return;
    mapInstance.current = L.map(mapRef.current, {
      center: [34, -6], zoom: 6,
      attributionControl: false,
      zoomControl: true,
    });
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 18,
      attribution: '&copy; OpenStreetMap',
    }).addTo(mapInstance.current);
    markersRef.current = L.layerGroup().addTo(mapInstance.current);
  }, []);

  useEffect(() => {
    if (!markersRef.current) return;
    markersRef.current.clearLayers();
    if (!locations.length) return;
    const bounds = [];
    locations.forEach((loc, idx) => {
      if (!loc.latitude || !loc.longitude) return;
      const color = COLORS[loc.attackType] || COLORS[loc.severity] || '#22c55e';
      const isNew = idx < locations.length && idx < 10;
      const marker = isNew
        ? L.marker([loc.latitude, loc.longitude], { icon: createPulsingIcon(color) })
        : L.circleMarker([loc.latitude, loc.longitude], {
            radius: 4, fillColor: color, color: '#fff', weight: 1, opacity: 0.7, fillOpacity: 0.5
          });
      marker.bindTooltip(`${loc.srcIp}<br/>${loc.attackType}${loc.severity ? '<br/>Severity: ' + loc.severity : ''}`, { direction: 'top' });
      markersRef.current.addLayer(marker);
      bounds.push([loc.latitude, loc.longitude]);
    });
    if (bounds.length > 0 && mapInstance.current) {
      mapInstance.current.fitBounds(bounds, { padding: [20, 20] });
    }
  }, [locations]);

  useEffect(() => {
    if (newAlertCount > 0) {
      const timer = setTimeout(() => setNewAlertCount(0), 5000);
      return () => clearTimeout(timer);
    }
  }, [newAlertCount]);

  return (
    <div style={{ background: '#1e293b', borderRadius: 12, padding: 20, marginTop: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <div style={{ fontSize: 14, fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: 1, display: 'flex', alignItems: 'center', gap: 8 }}>
          Live Traffic Map — All Attack Types ({loading ? 'loading...' : locations.length + ' locations'})
          {newAlertCount > 0 && (
            <span style={{
              background: '#dc2626', color: 'white', padding: '2px 8px', borderRadius: 10,
              fontSize: 11, fontWeight: 700, animation: 'pulse 1s infinite',
            }}>
              +{newAlertCount} new
            </span>
          )}
        </div>
        <div onClick={() => setShowPanel(p => !p)} style={{
          cursor: 'pointer', color: showPanel ? '#93c5fd' : '#64748b', fontSize: 12,
          display: 'flex', alignItems: 'center', gap: 4, padding: '4px 10px', borderRadius: 6,
          background: showPanel ? '#3b82f620' : 'transparent',
        }}>
          <AlertTriangle size={14} />
          Notifications ({notifications.length})
        </div>
      </div>

      {showPanel && notifications.length > 0 && (
        <div style={{
          background: '#0f172a', borderRadius: 8, padding: 10, marginBottom: 10,
          maxHeight: 160, overflowY: 'auto', border: '1px solid #1e293b',
        }}>
          {notifications.map(n => (
            <div key={n.id} style={{
              ...notificationStyle.notificationBar,
              background: `${SEVERITY_COLORS[n.severity] || '#64748b'}15`,
              borderLeft: `3px solid ${SEVERITY_COLORS[n.severity] || '#64748b'}`,
            }}>
              <span style={{
                width: 8, height: 8, borderRadius: '50%',
                background: SEVERITY_COLORS[n.severity] || '#64748b', flexShrink: 0,
              }} />
              <span style={{ color: '#64748b', fontSize: 11, flexShrink: 0 }}>{n.timestamp}</span>
              <span style={{ color: '#f1f5f9', fontWeight: 600, fontSize: 12 }}>{n.attackType}</span>
              <span style={{ color: '#94a3b8', fontSize: 11, fontFamily: 'monospace' }}>{n.srcIp}</span>
              <span style={{
                marginLeft: 'auto', padding: '1px 6px', borderRadius: 6, fontSize: 10, fontWeight: 600,
                background: `${SEVERITY_COLORS[n.severity] || '#64748b'}20`,
                color: SEVERITY_COLORS[n.severity] || '#64748b',
              }}>{n.severity.toUpperCase()}</span>
            </div>
          ))}
        </div>
      )}

      <div ref={mapRef} style={{ height: 450, borderRadius: 8, zIndex: 0 }} />
    </div>
  );
}
