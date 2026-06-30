import React, { useState, useEffect, useCallback } from 'react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, LineChart, Line, Legend, AreaChart, Area
} from 'recharts';
import { Shield, AlertTriangle, Activity, Server, RefreshCw, MapPin, TrendingUp } from 'lucide-react';
import { api, connectWebSocket } from '../services/api';
import GeoMap from './GeoMap';

const COLORS = {
  BENIGN: '#22c55e',
  'DoS Hulk': '#ef4444', DDoS: '#dc2626', PortScan: '#f59e0b',
  'Brute Force': '#f97316', Bot: '#a855f7',
  'Web Attack - XSS': '#ec4899', Infiltration: '#6366f1',
  normal: '#22c55e', attack: '#ef4444',
  high: '#ef4444', medium: '#f59e0b', low: '#3b82f6',
};
const PIE_COLORS = ['#22c55e', '#ef4444'];
const CICIDS_COLORS = ['#ef4444','#dc2626','#f59e0b','#f97316','#a855f7','#ec4899','#6366f1','#22c55e'];

function StatCard({ icon: Icon, label, value, color }) {
  return (
    <div style={{
      background: 'linear-gradient(135deg, #1e293b 0%, #0f172a 100%)', borderRadius: 12, padding: '20px 24px',
      display: 'flex', alignItems: 'center', gap: 16, flex: 1, minWidth: 180,
      border: '1px solid #334155', transition: 'transform 0.2s',
    }}>
      <div style={{
        width: 48, height: 48, borderRadius: 12,
        background: `${color}20`, display: 'flex', alignItems: 'center', justifyContent: 'center'
      }}>
        <Icon size={24} color={color} />
      </div>
      <div>
        <div style={{ color: '#94a3b8', fontSize: 12, marginBottom: 4, letterSpacing: 0.5 }}>{label}</div>
        <div style={{ color: '#f1f5f9', fontSize: 22, fontWeight: 700 }}>{value}</div>
      </div>
    </div>
  );
}

const styles = {
  container: { color: '#f1f5f9', fontFamily: "'Inter', -apple-system, sans-serif" },
  header: {
    background: 'linear-gradient(135deg, #1e293b 0%, #0f172a 100%)',
    borderBottom: '1px solid #334155', padding: '12px 32px',
    display: 'flex', alignItems: 'center', justifyContent: 'space-between', position: 'sticky', top: 0, zIndex: 50
  },
  title: { fontSize: 18, fontWeight: 700, display: 'flex', alignItems: 'center', gap: 10 },
  badge: { background: '#3b82f620', color: '#93c5fd', padding: '4px 12px', borderRadius: 20, fontSize: 11, fontWeight: 600 },
  content: { padding: 24, maxWidth: 1440, margin: '0 auto' },
  grid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(380px, 1fr))', gap: 20, marginTop: 24 },
  card: { background: '#1e293b', borderRadius: 12, padding: 20, border: '1px solid #334155' },
  cardTitle: { fontSize: 13, fontWeight: 600, color: '#94a3b8', marginBottom: 16, textTransform: 'uppercase', letterSpacing: 1 },
  table: { width: '100%', borderCollapse: 'collapse', fontSize: 13 },
  th: { textAlign: 'left', padding: '8px 12px', color: '#64748b', borderBottom: '1px solid #334155', fontWeight: 600, fontSize: 11, textTransform: 'uppercase', letterSpacing: 0.5 },
  td: { padding: '8px 12px', borderBottom: '1px solid #1e293b', color: '#cbd5e1' },
  alertBar: {
    background: '#dc2626', color: 'white', padding: '10px 24px',
    alignItems: 'center', gap: 12, fontSize: 14, fontWeight: 500,
    transition: 'max-height 0.3s, opacity 0.3s',
    overflow: 'hidden', animation: 'slideDown 0.3s ease-out',
  },
  legend: {
    display: 'flex', gap: 20, padding: '8px 0', flexWrap: 'wrap',
  },
  legendItem: { display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, color: '#94a3b8' },
  legendDot: { width: 10, height: 10, borderRadius: '50%', display: 'inline-block' },
};

export default function Dashboard() {
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [lastAlert, setLastAlert] = useState(null);
  const [realtimeAlerts, setRealtimeAlerts] = useState([]);
  const [geoLocations, setGeoLocations] = useState(0);
  const [attackTimeline, setAttackTimeline] = useState([]);

  const fetchDashboard = useCallback(async () => {
    try {
      const data = await api.getDashboard();
      setDashboard(data);
    } catch (err) { console.error(err) }
    finally { setLoading(false) }
  }, []);

  useEffect(() => {
    fetchDashboard();
    const i = setInterval(fetchDashboard, 10000);
    return () => clearInterval(i);
  }, [fetchDashboard]);

  useEffect(() => {
    const fetchGeoCount = async () => {
      try {
        const data = await api.getGeoLocations();
        setGeoLocations(data.length);
      } catch (e) {}
    };
    fetchGeoCount();
    const i = setInterval(fetchGeoCount, 30000);
    return () => clearInterval(i);
  }, []);

  useEffect(() => {
    const fetchTimeline = async () => {
      try {
        const data = await api.getAttackTimeline(60);
        setAttackTimeline(data);
      } catch (e) {}
    };
    fetchTimeline();
    const i = setInterval(fetchTimeline, 10000);
    return () => clearInterval(i);
  }, []);

  useEffect(() => {
    const client = connectWebSocket(
      (alert) => { setLastAlert(alert); setRealtimeAlerts(p => [alert, ...p].slice(0, 20)); },
      (stats) => setDashboard(stats)
    );
    return () => client.deactivate();
  }, []);

  const attackData = dashboard?.attack_distribution?.map(d => ({
    name: d.type, value: Number(d.count)
  })) || [];

  // Transform raw timeline rows [{minute, attack_type, cnt}, ...] -> [{minute, DoS Hulk:12, PortScan:5, ...}, ...]
  const timelineMap = {};
  (attackTimeline || []).forEach(r => {
    const m = r.minute ? new Date(r.minute).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '';
    if (!timelineMap[m]) timelineMap[m] = { minute: m };
    timelineMap[m][r.attack_type] = (timelineMap[m][r.attack_type] || 0) + Number(r.cnt);
  });
  const timelineChartData = Object.values(timelineMap).slice(-60);
  const attackTypes = [...new Set((attackTimeline || []).map(r => r.attack_type))];

  if (loading) {
    return (
      <div style={{
        minHeight: '100vh', background: '#0f172a', color: '#f1f5f9',
        display: 'flex', alignItems: 'center', justifyContent: 'center', flexDirection: 'column', gap: 16
      }}>
        <Shield size={48} color="#3b82f6" style={{ opacity: 0.5 }} />
        <div style={{ fontSize: 18 }}>Loading IDS Dashboard...</div>
      </div>
    );
  }

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16, width: '100%', justifyContent: 'flex-end' }}>
          <span style={{ display: 'flex', alignItems: 'center', gap: 4, color: '#64748b', fontSize: 12 }}>
            <MapPin size={12} /> {geoLocations}
          </span>
          <span style={styles.badge}>
            <TrendingUp size={12} style={{ marginRight: 4 }} />
            AI-Powered
          </span>
          <span style={{ color: '#64748b', fontSize: 12, display: 'flex', alignItems: 'center', gap: 4 }}>
            <span style={{ display: 'inline-block', width: 6, height: 6, background: '#22c55e', borderRadius: '50%', animation: 'pulse 2s infinite' }} />
            Live
          </span>
          <RefreshCw size={14} color="#64748b" style={{ cursor: 'pointer' }} onClick={fetchDashboard} />
        </div>
      </div>

      <div style={{ ...styles.alertBar, display: lastAlert ? 'flex' : 'none' }}>
        <AlertTriangle size={16} />
        {lastAlert
          ? `${lastAlert.severity?.toUpperCase()} ALERT: ${lastAlert.attackType} from ${lastAlert.srcIp}`
          : 'Monitoring network traffic...'}
      </div>

      <div style={styles.content}>
        <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
          <StatCard icon={AlertTriangle} label="Alerts (Last Hour)" value={dashboard?.alerts_last_hour?.toLocaleString() || 0} color="#ef4444" />
          <StatCard icon={Activity} label="Alerts (24h)" value={dashboard?.alerts_last_24h?.toLocaleString() || 0} color="#f59e0b" />
          <StatCard icon={Server} label="Total Packets" value={dashboard?.total_packets?.toLocaleString() || 0} color="#3b82f6" />
          <StatCard icon={Shield} label="Avg Attack % (24h)" value={`${(dashboard?.avg_attack_percentage_24h || 0).toFixed(1)}%`} color="#a855f7" />
        </div>

        <GeoMap />

        <div style={{ ...styles.card, marginTop: 16 }}>
          <div style={styles.cardTitle}>CICIDS2017 Attack Types</div>
          <div style={styles.legend}>
            <span style={styles.legendItem}><span style={{ ...styles.legendDot, background: '#22c55e' }} /> BENIGN</span>
            <span style={styles.legendItem}><span style={{ ...styles.legendDot, background: '#ef4444' }} /> DoS Hulk</span>
            <span style={styles.legendItem}><span style={{ ...styles.legendDot, background: '#dc2626' }} /> DDoS</span>
            <span style={styles.legendItem}><span style={{ ...styles.legendDot, background: '#f59e0b' }} /> PortScan</span>
            <span style={styles.legendItem}><span style={{ ...styles.legendDot, background: '#f97316' }} /> Brute Force</span>
            <span style={styles.legendItem}><span style={{ ...styles.legendDot, background: '#a855f7' }} /> Bot</span>
            <span style={styles.legendItem}><span style={{ ...styles.legendDot, background: '#ec4899' }} /> Web Attack</span>
            <span style={styles.legendItem}><span style={{ ...styles.legendDot, background: '#6366f1' }} /> Infiltration</span>
          </div>
        </div>

        <div style={styles.grid}>
          <div style={styles.card}>
            <div style={styles.cardTitle}>Traffic Distribution</div>
            <ResponsiveContainer width="100%" height={280}>
              <PieChart>
                <Pie data={[
                  { name: 'Normal', value: dashboard?.normal_percentage || 0 },
                  { name: 'Attack', value: dashboard?.attack_percentage || 0 }
                ]} cx="50%" cy="50%" innerRadius={60} outerRadius={100}
                   dataKey="value" label={({ name, value }) => `${name}: ${value.toFixed(1)}%`}>
                  {PIE_COLORS.map((c, i) => <Cell key={i} fill={c} />)}
                </Pie>
                <Tooltip contentStyle={{ background: '#1e293b', border: '1px solid #334155', borderRadius: 8 }} />
              </PieChart>
            </ResponsiveContainer>
          </div>

          <div style={styles.card}>
            <div style={styles.cardTitle}>Attack Distribution by Type</div>
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={attackData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                <XAxis dataKey="name" stroke="#64748b" tick={{ fontSize: 11 }} />
                <YAxis stroke="#64748b" />
                <Tooltip contentStyle={{ background: '#1e293b', border: '1px solid #334155', borderRadius: 8 }} />
                <Bar dataKey="value" radius={[4, 4, 0, 0]}>
                  {attackData.map((_, i) => <Cell key={i} fill={CICIDS_COLORS[i % CICIDS_COLORS.length]} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>

          <div style={styles.card}>
            <div style={styles.cardTitle}>Real-Time Attack Timeline (Last 60 min)</div>
            <ResponsiveContainer width="100%" height={280}>
              <AreaChart data={timelineChartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                <XAxis dataKey="minute" stroke="#64748b" tick={{ fontSize: 10 }} interval="preserveStartEnd" />
                <YAxis stroke="#64748b" />
                <Tooltip contentStyle={{ background: '#1e293b', border: '1px solid #334155', borderRadius: 8 }} />
                <Legend />
                {attackTypes.filter(t => t && t !== 'BENIGN' && t !== 'Normal').map((type, i) => (
                  <Area key={type} type="monotone" dataKey={type} stackId="1" stroke={COLORS[type] || '#6366f1'}
                    fill={COLORS[type] || '#6366f1'} fillOpacity={0.4} strokeWidth={1} dot={false} />
                ))}
              </AreaChart>
            </ResponsiveContainer>
          </div>

          <div style={styles.card}>
            <div style={styles.cardTitle}>Traffic Timeline (24h)</div>
            <ResponsiveContainer width="100%" height={280}>
              <LineChart data={dashboard?.traffic_timeline || []}>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                <XAxis dataKey="timestamp" stroke="#64748b" tick={{ fontSize: 10 }}
                  tickFormatter={(v) => v ? new Date(v).toLocaleTimeString() : ''} />
                <YAxis stroke="#64748b" domain={[0, 100]} />
                <Tooltip contentStyle={{ background: '#1e293b', border: '1px solid #334155', borderRadius: 8 }} />
                <Legend />
                <Line type="monotone" dataKey="attack_percentage" stroke="#ef4444" name="Attack %" strokeWidth={2} dot={false} />
                <Line type="monotone" dataKey="normal_percentage" stroke="#22c55e" name="Normal %" strokeWidth={2} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </div>

          <div style={styles.card}>
            <div style={styles.cardTitle}>Top Source IPs</div>
            <table style={styles.table}>
              <thead><tr>
                <th style={styles.th}>#</th>
                <th style={styles.th}>IP Address</th>
                <th style={styles.th}>Packets</th>
              </tr></thead>
              <tbody>
                {(dashboard?.top_ips || []).map((ip, i) => (
                  <tr key={i}>
                    <td style={{ ...styles.td, width: 30, color: '#64748b' }}>{i + 1}</td>
                    <td style={{ ...styles.td, fontFamily: 'monospace' }}>
                      <span style={{ display: 'inline-block', width: 8, height: 8, borderRadius: '50%', background: i < 3 ? '#ef4444' : '#f59e0b', marginRight: 8 }} />
                      {ip.ip}
                    </td>
                    <td style={styles.td}>{ip.count?.toLocaleString()}</td>
                  </tr>
                ))}
                {(!dashboard?.top_ips || dashboard.top_ips.length === 0) && (
                  <tr><td colSpan={3} style={{ ...styles.td, textAlign: 'center', color: '#64748b' }}>No data yet</td></tr>
                )}
              </tbody>
            </table>
          </div>

          <div style={styles.card}>
            <div style={styles.cardTitle}>Recent Real-Time Alerts</div>
            <div style={{ maxHeight: 280, overflowY: 'auto' }}>
              <table style={styles.table}>
                <thead><tr>
                  <th style={styles.th}>Time</th>
                  <th style={styles.th}>Type</th>
                  <th style={styles.th}>Source</th>
                  <th style={styles.th}>Severity</th>
                </tr></thead>
                <tbody>
                  {realtimeAlerts.length > 0 ? realtimeAlerts.map((a, i) => (
                    <tr key={i}>
                      <td style={styles.td}>{a.timestamp ? new Date(a.timestamp).toLocaleTimeString() : '--'}</td>
                      <td style={styles.td}>{a.attackType}</td>
                      <td style={{ ...styles.td, fontFamily: 'monospace' }}>{a.srcIp}</td>
                      <td style={styles.td}>
                        <span style={{
                          padding: '2px 8px', borderRadius: 12, fontSize: 11, fontWeight: 600,
                          background: a.severity === 'high' ? '#dc262620' : '#3b82f620',
                          color: a.severity === 'high' ? '#fca5a5' : '#93c5fd'
                        }}>
                          {a.severity?.toUpperCase() || 'INFO'}
                        </span>
                      </td>
                    </tr>
                  )) : (
                    <tr><td colSpan={4} style={{ ...styles.td, textAlign: 'center', color: '#64748b' }}>Waiting for alerts...</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          <div style={styles.card}>
            <div style={styles.cardTitle}>Attack Summaries</div>
            <table style={styles.table}>
              <thead><tr>
                <th style={styles.th}>Type</th>
                <th style={styles.th}>Count</th>
                <th style={styles.th}>Confidence</th>
                <th style={styles.th}>Last Seen</th>
              </tr></thead>
              <tbody>
                {(dashboard?.attack_summaries || []).map((s, i) => (
                  <tr key={i}>
                    <td style={styles.td}>{s.attackType}</td>
                    <td style={styles.td}>{s.count?.toLocaleString()}</td>
                    <td style={styles.td}>{(s.avgConfidence * 100).toFixed(1)}%</td>
                    <td style={styles.td}>{s.lastSeen ? new Date(s.lastSeen).toLocaleTimeString() : '--'}</td>
                  </tr>
                ))}
                {(!dashboard?.attack_summaries || dashboard.attack_summaries.length === 0) && (
                  <tr><td colSpan={4} style={{ ...styles.td, textAlign: 'center', color: '#64748b' }}>No summaries yet</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
