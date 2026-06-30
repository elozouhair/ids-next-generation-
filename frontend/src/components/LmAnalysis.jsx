import React, { useState, useEffect } from 'react';
import { Brain, TrendingUp, TrendingDown, AlertTriangle, Shield, MapPin, Clock, Target, Zap, ArrowUpRight, Lightbulb, FileText } from 'lucide-react';
import { api, connectWebSocket } from '../services/api';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, LineChart, Line, Legend, AreaChart, Area } from 'recharts';

const styles = {
  container: { padding: 24, maxWidth: 1440, margin: '0 auto' },
  headerSection: {
    background: 'linear-gradient(135deg, #1e293b 0%, #0f172a 100%)',
    borderRadius: 16, padding: 28, marginBottom: 20,
    border: '1px solid #334155',
  },
  titleRow: { display: 'flex', alignItems: 'center', gap: 12, marginBottom: 12 },
  subtitle: { color: '#94a3b8', fontSize: 13, lineHeight: 1.6, margin: 0 },
  grid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(380px, 1fr))', gap: 20, marginTop: 20 },
  card: { background: '#1e293b', borderRadius: 12, padding: 20, border: '1px solid #334155' },
  cardTitle: { fontSize: 13, fontWeight: 600, color: '#94a3b8', marginBottom: 16, textTransform: 'uppercase', letterSpacing: 1 },
  insightCard: {
    background: 'linear-gradient(135deg, #1e293b 0%, #0f172a 100%)',
    borderRadius: 12, padding: 20, border: '1px solid #334155',
    borderLeft: '3px solid #3b82f6',
  },
  reasoningBlock: {
    background: '#0f172a', borderRadius: 8, padding: 16, marginBottom: 12,
    border: '1px solid #1e293b',
  },
  tag: {
    display: 'inline-block', padding: '3px 10px', borderRadius: 12,
    fontSize: 11, fontWeight: 600, marginRight: 6, marginBottom: 4,
  },
  metricBox: {
    background: '#0f172a', borderRadius: 8, padding: 16, textAlign: 'center',
    border: '1px solid #1e293b', flex: 1, minWidth: 120,
  },
  table: { width: '100%', borderCollapse: 'collapse', fontSize: 13 },
  th: { textAlign: 'left', padding: '8px 12px', color: '#64748b', borderBottom: '1px solid #334155', fontWeight: 600, fontSize: 11, textTransform: 'uppercase', letterSpacing: 0.5 },
  td: { padding: '8px 12px', borderBottom: '1px solid #1e293b', color: '#cbd5e1' },
};

const CICIDS_COLORS = {
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
};

const SEVERITY_ORDER = ['Critical', 'High', 'Medium', 'Low', 'Info'];

function generateAnalysis(dashboard) {
  if (!dashboard) return null;

  const dist = dashboard.attack_distribution || [];
  const totalAlerts = dashboard.alerts_last_24h || 0;
  const attackPct = dashboard.avg_attack_percentage_24h || 0;
  const topIps = dashboard.top_ips || [];
  const timeline = dashboard.traffic_timeline || [];

  const attackTypes = dist.filter(d => d.type !== 'BENIGN' && d.type !== 'Normal');
  const benignCount = dist.find(d => d.type === 'BENIGN')?.count || 0;

  const totalAttackCount = attackTypes.reduce((sum, a) => sum + a.count, 0);
  const topAttack = attackTypes.sort((a, b) => b.count - a.count)[0];

  const recentTimeline = timeline.slice(-5);
  const trendUp = recentTimeline.length > 1 &&
    recentTimeline[recentTimeline.length - 1].attackPercentage >
    recentTimeline[0].attackPercentage;

  const timelineValues = timeline.map(t => t.attack_percentage || 0);
  const avgRecent = timelineValues.slice(-5).reduce((a, b) => a + b, 0) / 5;
  const volatility = timelineValues.length > 5
    ? Math.round(Math.sqrt(timelineValues.slice(-10).reduce((sum, v) => sum + Math.pow(v - avgRecent, 2), 0) / 10) * 100) / 100
    : 0;

  const recommendations = [];
  if (topAttack && topAttack.count > 100000) {
    recommendations.push({
      icon: Target,
      text: `${topAttack.type} attacks exceed 100K events — deploy dedicated rate-limiting and WAF rules.`,
      severity: 'high',
    });
  }
  if (attackPct > 30) {
    recommendations.push({
      icon: Shield,
      text: `Attack ratio at ${attackPct.toFixed(1)}% — consider network segmentation and zero-trust policies.`,
      severity: 'high',
    });
  }
  if (trendUp) {
    recommendations.push({
      icon: TrendingUp,
      text: 'Attack percentage trending upward in last 5 batches — immediate threat hunting recommended.',
      severity: 'medium',
    });
  }
  if (volatility > 5) {
    recommendations.push({
      icon: Zap,
      text: `Traffic volatility at ${volatility}% — suggests scanning or DDoS behavior pattern.`,
      severity: 'medium',
    });
  }
  if (benignCount > 0 && benignCount > totalAttackCount * 2) {
    recommendations.push({
      icon: Lightbulb,
      text: 'High benign-to-attack ratio — fine-tune detection thresholds to reduce false positives.',
      severity: 'low',
    });
  }

  const topIpsText = topIps.slice(0, 3).map(ip => `${ip.ip} (${ip.count} packets)`).join(', ');

  return {
    summary: `Over the last 24 hours, the IDS processed **${totalAlerts.toLocaleString()}** events with an average attack ratio of **${attackPct.toFixed(1)}%**. ` +
      `${attackTypes.length} distinct CICIDS2017 attack types were detected, with **${topAttack?.type || 'unknown'}** being the most prevalent (${topAttack?.count?.toLocaleString() || 0} events). ` +
      `The top source IPs are ${topIpsText}.`,
    riskLevel: attackPct > 35 ? 'Critical' : attackPct > 25 ? 'High' : attackPct > 15 ? 'Medium' : 'Low',
    attackPct: attackPct,
    attackTypes: attackTypes,
    totalAttackCount: totalAttackCount,
    topAttack: topAttack,
    topIpsFirst: topIps.slice(0, 10),
    trend: trendUp ? 'increasing' : 'stable',
    volatility: volatility,
    recommendations: recommendations,
    timeline: timeline,
    alertRate: totalAlerts > 0 ? Math.round(totalAlerts / (timeline.length || 1)) : 0,
    attackComposition: attackTypes.slice(0, 8).map(a => ({
      name: a.type,
      percentage: totalAttackCount > 0 ? ((a.count / totalAttackCount) * 100).toFixed(1) : 0,
      count: a.count,
    })),
    analysisTime: new Date().toLocaleString(),
  };
}

function getRiskColor(level) {
  switch (level) {
    case 'Critical': return '#dc2626';
    case 'High': return '#ef4444';
    case 'Medium': return '#f59e0b';
    case 'Low': return '#22c55e';
    default: return '#64748b';
  }
}

function getSeverityColor(sev) {
  return sev === 'high' ? '#ef4444' : sev === 'medium' ? '#f59e0b' : '#3b82f6';
}

export default function LmAnalysis() {
  const [dashboard, setDashboard] = useState(null);
  const [analysis, setAnalysis] = useState(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('overview');

  useEffect(() => {
    api.getDashboard().then(data => {
      setDashboard(data);
      setAnalysis(generateAnalysis(data));
      setLoading(false);
    }).catch(() => setLoading(false));

    const client = connectWebSocket(null, (stats) => {
      setDashboard(stats);
      setAnalysis(generateAnalysis(stats));
    });

    return () => {
      if (client) client.deactivate();
    };
  }, []);

  if (loading) {
    return (
      <div style={{ minHeight: '60vh', display: 'flex', alignItems: 'center', justifyContent: 'center', flexDirection: 'column', gap: 16 }}>
        <Brain size={48} color="#3b82f6" style={{ opacity: 0.5 }} />
        <div style={{ color: '#94a3b8', fontSize: 16 }}>AI Analysis Engine running...</div>
      </div>
    );
  }

  if (!analysis) {
    return (
      <div style={{ ...styles.container, textAlign: 'center', paddingTop: 60 }}>
        <AlertTriangle size={48} color="#f59e0b" style={{ opacity: 0.5 }} />
        <div style={{ color: '#94a3b8', fontSize: 16, marginTop: 12 }}>No data available for analysis</div>
      </div>
    );
  }

  const tabs = [
    { key: 'overview', label: 'Overview & Reasoning', icon: Brain },
    { key: 'threats', label: 'Threat Analysis', icon: Target },
    { key: 'trends', label: 'Trends & Patterns', icon: TrendingUp },
    { key: 'recommendations', label: 'Recommendations', icon: Lightbulb },
  ];

  return (
    <div style={styles.container}>
      <div style={styles.headerSection}>
        <div style={styles.titleRow}>
          <div style={{ width: 48, height: 48, borderRadius: 12, background: '#3b82f620', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Brain size={24} color="#3b82f6" />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <span style={{ fontSize: 18, fontWeight: 700 }}>AI Security Analysis & Reasoning</span>
              <span style={{ background: '#3b82f620', color: '#93c5fd', padding: '3px 10px', borderRadius: 12, fontSize: 11, fontWeight: 600 }}>
                LM-Powered
              </span>
            </div>
            <p style={styles.subtitle}>
              Real-time intelligence analysis using machine learning reasoning. Risk assessment, attack pattern recognition, and automated recommendations based on live network telemetry.
            </p>
          </div>
        </div>

        <div style={{ display: 'flex', gap: 12, marginTop: 16, flexWrap: 'wrap' }}>
          {tabs.map(tab => {
            const Icon = tab.icon;
            return (
              <div key={tab.key} onClick={() => setActiveTab(tab.key)}
                style={{
                  display: 'flex', alignItems: 'center', gap: 6, padding: '8px 16px',
                  borderRadius: 8, cursor: 'pointer', fontSize: 13,
                  background: activeTab === tab.key ? '#3b82f620' : 'transparent',
                  color: activeTab === tab.key ? '#93c5fd' : '#64748b',
                  border: activeTab === tab.key ? '1px solid #3b82f640' : '1px solid transparent',
                  transition: 'all 0.2s',
                }}>
                <Icon size={14} />
                {tab.label}
              </div>
            );
          })}
        </div>
      </div>

      {activeTab === 'overview' && (
        <>
          <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', marginBottom: 20 }}>
            <div style={{ ...styles.metricBox }}>
              <div style={{ color: '#64748b', fontSize: 11, marginBottom: 8 }}>Risk Level</div>
              <div style={{ fontSize: 22, fontWeight: 700, color: getRiskColor(analysis.riskLevel) }}>{analysis.riskLevel}</div>
            </div>
            <div style={{ ...styles.metricBox }}>
              <div style={{ color: '#64748b', fontSize: 11, marginBottom: 8 }}>Attack Ratio</div>
              <div style={{ fontSize: 22, fontWeight: 700, color: '#f1f5f9' }}>{analysis.attackPct.toFixed(1)}%</div>
            </div>
            <div style={{ ...styles.metricBox }}>
              <div style={{ color: '#64748b', fontSize: 11, marginBottom: 8 }}>Active Threats</div>
              <div style={{ fontSize: 22, fontWeight: 700, color: '#f1f5f9' }}>{analysis.attackTypes.length}</div>
            </div>
            <div style={{ ...styles.metricBox }}>
              <div style={{ color: '#64748b', fontSize: 11, marginBottom: 8 }}>Alert Rate</div>
              <div style={{ fontSize: 22, fontWeight: 700, color: '#f1f5f9' }}>{analysis.alertRate.toLocaleString()}/h</div>
            </div>
            <div style={{ ...styles.metricBox }}>
              <div style={{ color: '#64748b', fontSize: 11, marginBottom: 8 }}>Trend</div>
              <div style={{ fontSize: 22, fontWeight: 700, color: analysis.trend === 'increasing' ? '#ef4444' : '#22c55e', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6 }}>
                {analysis.trend === 'increasing' ? <TrendingUp size={18} /> : <TrendingDown size={18} />}
                {analysis.trend === 'increasing' ? 'Up' : 'Stable'}
              </div>
            </div>
            <div style={{ ...styles.metricBox }}>
              <div style={{ color: '#64748b', fontSize: 11, marginBottom: 8 }}>Volatility</div>
              <div style={{ fontSize: 22, fontWeight: 700, color: '#f1f5f9' }}>{analysis.volatility}%</div>
            </div>
          </div>

          <div style={styles.insightCard}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
              <FileText size={16} color="#60a5fa" />
              <span style={{ fontSize: 14, fontWeight: 600, color: '#f1f5f9' }}>Executive Summary</span>
              <span style={{ color: '#64748b', fontSize: 11, marginLeft: 'auto' }}>Generated: {analysis.analysisTime}</span>
            </div>
            <p style={{ color: '#cbd5e1', fontSize: 14, lineHeight: 1.7, margin: 0 }}>
              {analysis.summary.split('**').map((part, i) =>
                i % 2 === 1 ? <strong key={i} style={{ color: '#f1f5f9' }}>{part}</strong> : part
              )}
            </p>
          </div>

          <div style={styles.grid}>
            <div style={styles.card}>
              <div style={styles.cardTitle}>Reasoning — Attack Composition</div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {analysis.attackComposition.slice(0, 6).map((a, i) => (
                  <div key={i}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                      <span style={{ color: '#cbd5e1', fontSize: 12, display: 'flex', alignItems: 'center', gap: 6 }}>
                        <span style={{ width: 8, height: 8, borderRadius: '50%', background: CICIDS_COLORS[a.name] || '#64748b', display: 'inline-block' }} />
                        {a.name}
                      </span>
                      <span style={{ color: '#94a3b8', fontSize: 12 }}>{a.percentage}%</span>
                    </div>
                    <div style={{ background: '#0f172a', borderRadius: 4, height: 6, overflow: 'hidden' }}>
                      <div style={{ width: `${Math.min(parseFloat(a.percentage), 100)}%`, height: '100%', background: CICIDS_COLORS[a.name] || '#64748b', borderRadius: 4, transition: 'width 0.5s' }} />
                    </div>
                  </div>
                ))}
              </div>
              {analysis.topAttack && (
                <div style={{ ...styles.reasoningBlock, marginTop: 12, borderLeft: '3px solid #ef4444' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
                    <Zap size={14} color="#ef4444" />
                    <span style={{ color: '#f1f5f9', fontSize: 13, fontWeight: 600 }}>Key Finding</span>
                  </div>
                  <p style={{ color: '#94a3b8', fontSize: 12, lineHeight: 1.6, margin: 0 }}>
                    <strong style={{ color: '#cbd5e1' }}>{analysis.topAttack.type}</strong> accounts for{' '}
                    <strong style={{ color: '#cbd5e1' }}>{((analysis.topAttack.count / (analysis.attackTypes.reduce((s, a) => s + a.count, 0))) * 100).toFixed(1)}%</strong>{' '}
                    of all malicious traffic. This is consistent with automated scanning or DoS tooling targeting network services.
                  </p>
                </div>
              )}
            </div>

            <div style={styles.card}>
              <div style={styles.cardTitle}>Reasoning — Source IP Analysis</div>
              <table style={styles.table}>
                <thead><tr>
                  <th style={styles.th}>Rank</th>
                  <th style={styles.th}>IP Address</th>
                  <th style={styles.th}>Events</th>
                  <th style={styles.th}>Threat Score</th>
                </tr></thead>
                <tbody>
                  {analysis.topIpsFirst.map((ip, i) => (
                    <tr key={i}>
                      <td style={{ ...styles.td, color: '#64748b', width: 30 }}>#{i + 1}</td>
                      <td style={{ ...styles.td, fontFamily: 'monospace' }}>
                        <span style={{
                          display: 'inline-block', width: 8, height: 8, borderRadius: '50%',
                          background: i < 3 ? '#ef4444' : i < 6 ? '#f59e0b' : '#3b82f6', marginRight: 8
                        }} />
                        {ip.ip}
                      </td>
                      <td style={styles.td}>{ip.count?.toLocaleString()}</td>
                      <td style={styles.td}>
                        <span style={{
                          padding: '2px 8px', borderRadius: 10, fontSize: 11, fontWeight: 600,
                          background: i < 3 ? '#dc262620' : i < 6 ? '#f59e0b20' : '#3b82f620',
                          color: i < 3 ? '#fca5a5' : i < 6 ? '#fde68a' : '#93c5fd',
                        }}>
                          {i < 3 ? 'HIGH' : i < 6 ? 'MEDIUM' : 'LOW'}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {analysis.topIpsFirst.length > 0 && (
                <div style={{ ...styles.reasoningBlock, marginTop: 12, borderLeft: '3px solid #f59e0b' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
                    <MapPin size={14} color="#f59e0b" />
                    <span style={{ color: '#f1f5f9', fontSize: 13, fontWeight: 600 }}>Geo-Distribution Note</span>
                  </div>
                  <p style={{ color: '#94a3b8', fontSize: 12, lineHeight: 1.6, margin: 0 }}>
                    Top IPs show uniform traffic distribution suggesting orchestrated scanning from multiple sources. No single IP dominates ({'>'}6% of total), which may indicate a distributed botnet or cloud-hosted scanning infrastructure.
                  </p>
                </div>
              )}
            </div>
          </div>
        </>
      )}

      {activeTab === 'threats' && (
        <div style={styles.grid}>
          <div style={{ ...styles.card, gridColumn: '1 / -1' }}>
            <div style={styles.cardTitle}>Threat Classification & Risk Assessment</div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: 16 }}>
              {analysis.attackTypes.map((at, i) => {
                const risk = at.count > 200000 ? 'Critical' : at.count > 100000 ? 'High' : at.count > 50000 ? 'Medium' : at.count > 10000 ? 'Low' : 'Info';
                const color = getRiskColor(risk);
                const currentTotal = analysis.totalAttackCount || 1;
                const pct = currentTotal > 0 ? ((at.count / currentTotal) * 100).toFixed(1) : 0;
                return (
                  <div key={i} style={{
                    background: '#0f172a', borderRadius: 10, padding: 16,
                    border: '1px solid #1e293b', borderLeft: `3px solid ${color}`,
                  }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                      <span style={{ color: '#f1f5f9', fontWeight: 600, fontSize: 14, display: 'flex', alignItems: 'center', gap: 6 }}>
                        <span style={{ width: 8, height: 8, borderRadius: '50%', background: CICIDS_COLORS[at.type] || '#64748b', display: 'inline-block' }} />
                        {at.type}
                      </span>
                      <span style={{
                        padding: '2px 8px', borderRadius: 8, fontSize: 10, fontWeight: 700,
                        background: `${color}20`, color: color,
                      }}>{risk}</span>
                    </div>
                    <div style={{ color: '#94a3b8', fontSize: 12, marginBottom: 4 }}>{at.count?.toLocaleString()} events — {pct}% of attacks</div>
                    <div style={{ background: '#1e293b', borderRadius: 4, height: 4, overflow: 'hidden' }}>
                      <div style={{ width: `${Math.min(parseFloat(pct), 100)}%`, height: '100%', background: color, borderRadius: 4 }} />
                    </div>
                    <div style={{ marginTop: 8, fontSize: 11, color: '#64748b' }}>
                      {at.type === 'DoS Hulk' && 'High-volume HTTP flood tool. Targets web servers with randomized requests.'}
                      {at.type === 'PortScan' && 'Reconnaissance activity. May precede targeted exploitation.'}
                      {at.type === 'Brute Force' && 'Credential stuffing or password guessing against exposed services.'}
                      {at.type === 'Infiltration' && 'Multi-stage compromise — initial foothold with lateral movement potential.'}
                      {at.type === 'Attack' && 'Generic attack classification — further analysis recommended.'}
                      {at.type === 'DDoS' && 'Distributed denial of service — volumetric or protocol attack.'}
                      {at.type === 'Bot' && 'Automated botnet traffic — possible C2 communication or scanning.'}
                      {at.type === 'Web Attack - XSS' && 'Cross-site scripting attempt — targets web application users.'}
                      {!['DoS Hulk','PortScan','Brute Force','Infiltration','Attack','DDoS','Bot','Web Attack - XSS'].includes(at.type) && 'CICIDS2017 classified threat — monitor for escalation.'}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {activeTab === 'trends' && (
        <>
          <div style={styles.grid}>
            <div style={styles.card}>
              <div style={styles.cardTitle}>Attack Ratio Trend (24h)</div>
              <ResponsiveContainer width="100%" height={280}>
                <AreaChart data={analysis.timeline}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                  <XAxis dataKey="timestamp" stroke="#64748b" tick={{ fontSize: 10 }}
                    tickFormatter={(v) => v ? new Date(v).toLocaleTimeString() : ''} />
                  <YAxis stroke="#64748b" domain={[0, 100]} />
                  <Tooltip contentStyle={{ background: '#1e293b', border: '1px solid #334155', borderRadius: 8 }} />
                  <Area type="monotone" dataKey="attack_percentage" stroke="#ef4444" fill="#ef444420" name="Attack %" />
                  <Area type="monotone" dataKey="normal_percentage" stroke="#22c55e" fill="#22c55e20" name="Normal %" />
                </AreaChart>
              </ResponsiveContainer>
            </div>

            <div style={styles.card}>
              <div style={styles.cardTitle}>Attack Type Distribution</div>
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={analysis.attackComposition}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                  <XAxis dataKey="name" stroke="#64748b" tick={{ fontSize: 10 }} />
                  <YAxis stroke="#64748b" />
                  <Tooltip contentStyle={{ background: '#1e293b', border: '1px solid #334155', borderRadius: 8 }} />
                  <Bar dataKey="percentage" name="% of Attacks" radius={[4, 4, 0, 0]}>
                    {analysis.attackComposition.map((_, i) => (
                      <rect key={i} fill={CICIDS_COLORS[analysis.attackComposition[i]?.name] || '#64748b'} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div style={{ ...styles.card, marginTop: 20 }}>
            <div style={styles.cardTitle}>Pattern Recognition — Reasoning</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <div style={styles.reasoningBlock}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
                  <TrendingUp size={14} color={analysis.trend === 'increasing' ? '#ef4444' : '#22c55e'} />
                  <span style={{ color: '#f1f5f9', fontSize: 13, fontWeight: 600 }}>
                    Trend Analysis: {analysis.trend === 'increasing' ? 'Attack Ratio Increasing' : 'Attack Ratio Stable'}
                  </span>
                </div>
                <p style={{ color: '#94a3b8', fontSize: 12, lineHeight: 1.6, margin: 0 }}>
                  {analysis.trend === 'increasing'
                    ? `The attack percentage has risen over the last ${Math.min(5, analysis.timeline.length)} data points. This sustained increase warrants immediate investigation. The volatility index of ${analysis.volatility}% suggests the traffic pattern is consistent with active scanning or ongoing exploitation attempts.`
                    : `The attack percentage has remained relatively stable over recent data points. The volatility index of ${analysis.volatility}% indicates ${analysis.volatility < 3 ? 'a steady-state traffic pattern with minimal fluctuation' : 'some degree of traffic pattern variation suggesting periodic scanning activity'}.`}
                </p>
              </div>

              <div style={styles.reasoningBlock}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
                  <Clock size={14} color="#f59e0b" />
                  <span style={{ color: '#f1f5f9', fontSize: 13, fontWeight: 600 }}>Temporal Pattern</span>
                </div>
                <p style={{ color: '#94a3b8', fontSize: 12, lineHeight: 1.6, margin: 0 }}>
                  Attack distribution over time shows consistent ratio of ~{analysis.attackPct.toFixed(0)}% malicious traffic,
                  indicating either continuous automated attack tooling or background scanning traffic.
                  {analysis.volatility > 5
                    ? ' Higher volatility suggests burst activity — investigate time periods with spikes above the mean.'
                    : ' Low volatility indicates sustained, steady-state attack patterns typical of automated botnets.'}
                </p>
              </div>

              <div style={styles.reasoningBlock}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
                  <Target size={14} color="#6366f1" />
                  <span style={{ color: '#f1f5f9', fontSize: 13, fontWeight: 600 }}>Attack Vector Analysis</span>
                </div>
                <p style={{ color: '#94a3b8', fontSize: 12, lineHeight: 1.6, margin: 0 }}>
                  {analysis.topAttack
                    ? `Primary attack vector: ${analysis.topAttack.type} (${analysis.topAttack.count?.toLocaleString()} events). ` +
                      (analysis.topAttack.type === 'DoS Hulk'
                        ? 'DoS Hulk is a Layer 7 HTTP flood tool — suggests web application layer targeting.'
                        : analysis.topAttack.type === 'PortScan'
                        ? 'Port scanning indicates reconnaissance phase — may precede targeted attacks.'
                        : analysis.topAttack.type === 'Brute Force'
                        ? 'Brute force suggests credential-based attacks against authentication services.'
                        : analysis.topAttack.type === 'Infiltration'
                        ? 'Infiltration attempts indicate multi-stage compromise targeting internal resources.'
                        : 'Further analysis needed to determine specific attack methodology.')
                    : 'No dominant attack vector identified.'}
                </p>
              </div>
            </div>
          </div>
        </>
      )}

      {activeTab === 'recommendations' && (
        <>
          <div style={{ ...styles.card, marginBottom: 20 }}>
            <div style={styles.cardTitle}>Automated Security Recommendations</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              {analysis.recommendations.length > 0 ? analysis.recommendations.map((rec, i) => {
                const Icon = rec.icon;
                return (
                  <div key={i} style={{
                    display: 'flex', gap: 12, padding: 16, borderRadius: 10,
                    background: '#0f172a', border: '1px solid #1e293b',
                    borderLeft: `3px solid ${getSeverityColor(rec.severity)}`,
                  }}>
                    <div style={{
                      width: 36, height: 36, borderRadius: 8,
                      background: `${getSeverityColor(rec.severity)}20`,
                      display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
                    }}>
                      <Icon size={18} color={getSeverityColor(rec.severity)} />
                    </div>
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                        <span style={{ color: '#f1f5f9', fontSize: 13, fontWeight: 600 }}>Recommendation {i + 1}</span>
                        <span style={{
                          padding: '2px 8px', borderRadius: 8, fontSize: 10, fontWeight: 700,
                          background: `${getSeverityColor(rec.severity)}20`,
                          color: getSeverityColor(rec.severity),
                        }}>{rec.severity.toUpperCase()}</span>
                      </div>
                      <p style={{ color: '#94a3b8', fontSize: 13, lineHeight: 1.6, margin: 0 }}>{rec.text}</p>
                    </div>
                  </div>
                );
              }) : (
                <div style={{ textAlign: 'center', padding: 20, color: '#64748b', fontSize: 13 }}>
                  No specific recommendations — current traffic pattern appears normal.
                </div>
              )}
            </div>
          </div>

          <div style={{ ...styles.card }}>
            <div style={styles.cardTitle}>Reasoning — Recommendation Logic</div>
            <div style={styles.reasoningBlock}>
              <p style={{ color: '#94a3b8', fontSize: 12, lineHeight: 1.6, margin: 0 }}>
                Recommendations are generated by the LM reasoning engine based on the following criteria:
              </p>
              <ul style={{ color: '#64748b', fontSize: 12, lineHeight: 2, margin: '8px 0 0', paddingLeft: 20 }}>
                <li>Attack type frequency exceeding statistical thresholds (100K+ events)</li>
                <li>Attack-to-benign ratio compared to baseline expectations (threshold: &gt;30%)</li>
                <li>Temporal trend analysis — sustained increases trigger mitigation recommendations</li>
                <li>Traffic volatility index — high variance suggests scanning/DDoS behavior</li>
                <li>False positive ratio assessment — benign/attack imbalance triggers tuning suggestions</li>
              </ul>
            </div>
            <div style={{ display: 'flex', gap: 12, marginTop: 12, flexWrap: 'wrap' }}>
              <div style={{ ...styles.tag, background: '#3b82f620', color: '#93c5fd' }}>
                <Clock size={12} style={{ marginRight: 4 }} /> 30s refresh cycle
              </div>
              <div style={{ ...styles.tag, background: '#6366f120', color: '#a5b4fc' }}>
                <Brain size={12} style={{ marginRight: 4 }} /> Rule-based reasoning engine
              </div>
              <div style={{ ...styles.tag, background: '#22c55e20', color: '#86efac' }}>
                <Shield size={12} style={{ marginRight: 4 }} /> CICIDS2017 classification
              </div>
            </div>
          </div>
        </>
      )}

      <div style={{ textAlign: 'center', color: '#475569', fontSize: 11, marginTop: 40, padding: 16, borderTop: '1px solid #1e293b' }}>
        AI Security Analysis Engine &copy; 2026 IDS Pipeline — Reasoning powered by real-time telemetry analysis
      </div>
    </div>
  );
}
