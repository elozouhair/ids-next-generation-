import React, { useState, useEffect } from 'react';
import { Shield, Github, ArrowUp, Brain, LayoutDashboard } from 'lucide-react';
import Dashboard from './components/Dashboard';
import LmAnalysis from './components/LmAnalysis';

function Footer() {
  const [showScroll, setShowScroll] = useState(false);
  useEffect(() => {
    const handler = () => setShowScroll(window.scrollY > 400);
    window.addEventListener('scroll', handler);
    return () => window.removeEventListener('scroll', handler);
  }, []);

  return (
    <>
      {showScroll && (
        <div onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
          style={{ position: 'fixed', bottom: 24, right: 24, width: 40, height: 40, borderRadius: '50%', background: '#3b82f6', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', zIndex: 100, boxShadow: '0 4px 12px rgba(59,130,246,0.4)', transition: 'transform 0.2s' }}>
          <ArrowUp size={20} color="white" />
        </div>
      )}
      <footer style={{
        background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)',
        borderTop: '1px solid #334155', padding: '32px 24px 24px', marginTop: 40
      }}>
        <div style={{ maxWidth: 1440, margin: '0 auto', display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', gap: 24 }}>
          <div style={{ minWidth: 240 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
              <Shield size={24} color="#3b82f6" />
              <span style={{ fontSize: 16, fontWeight: 700, color: '#f1f5f9' }}>IDS</span>
            </div>
            <p style={{ color: '#64748b', fontSize: 13, lineHeight: 1.6, margin: 0 }}>
              Real-time network intrusion detection powered by Apache Spark ML, PostGIS GeoIP, and Kafka streaming.
            </p>
          </div>
          <div>
            <div style={{ fontSize: 12, fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 12 }}>System</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <span style={{ color: '#64748b', fontSize: 13 }}>Apache Spark 3.5</span>
              <span style={{ color: '#64748b', fontSize: 13 }}>Kafka 3.6</span>
              <span style={{ color: '#64748b', fontSize: 13 }}>PostgreSQL 15 + PostGIS</span>
              <span style={{ color: '#64748b', fontSize: 13 }}>Grafana 11</span>
            </div>
          </div>
          <div>
            <div style={{ fontSize: 12, fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 12 }}>Resources</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <a href="http://localhost:3001" target="_blank" rel="noopener noreferrer" style={{ color: '#60a5fa', fontSize: 13, textDecoration: 'none' }}>Grafana Dashboard</a>
              <a href="http://localhost:8080" target="_blank" rel="noopener noreferrer" style={{ color: '#60a5fa', fontSize: 13, textDecoration: 'none' }}>Spark Master UI</a>
              <a href="http://localhost:8081" target="_blank" rel="noopener noreferrer" style={{ color: '#60a5fa', fontSize: 13, textDecoration: 'none' }}>Spark Worker UI</a>
            </div>
          </div>
        </div>
        <div style={{ maxWidth: 1440, margin: '24px auto 0', paddingTop: 16, borderTop: '1px solid #1e293b', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
          <span style={{ color: '#475569', fontSize: 12 }}>
            &copy; 2026 IDS Pipeline. All rights reserved.
          </span>
          <div style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
            <span style={{ color: '#475569', fontSize: 12 }}>Built with Spark &middot; Kafka &middot; PostGIS</span>
            <a href="https://github.com" target="_blank" rel="noopener noreferrer" style={{ color: '#64748b' }}>
              <Github size={16} />
            </a>
          </div>
        </div>
      </footer>
    </>
  );
}

const navBtn = (active, label, icon) => ({
  display: 'flex', alignItems: 'center', gap: 6, padding: '8px 16px',
  borderRadius: 8, cursor: 'pointer', fontSize: 13, fontWeight: 500,
  background: active ? '#3b82f620' : 'transparent',
  color: active ? '#93c5fd' : '#64748b',
  border: active ? '1px solid #3b82f640' : '1px solid transparent',
  transition: 'all 0.2s',
});

export default function App() {
  const [page, setPage] = useState('dashboard');

  return (
    <div style={{ minHeight: '100vh', background: '#0f172a', color: '#f1f5f9', fontFamily: "'Inter', -apple-system, sans-serif" }}>
      <div style={{
        background: 'linear-gradient(135deg, #1e293b 0%, #0f172a 100%)',
        borderBottom: '1px solid #334155', padding: '10px 32px',
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        position: 'sticky', top: 0, zIndex: 100,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <Shield size={22} color="#3b82f6" />
          <span style={{ fontSize: 15, fontWeight: 700 }}>IDS Pipeline</span>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <div style={navBtn(page === 'dashboard', 'Dashboard', LayoutDashboard)}
            onClick={() => setPage('dashboard')}>
            <LayoutDashboard size={14} />
            Dashboard
          </div>
          <div style={navBtn(page === 'lm', 'LM Analysis', Brain)}
            onClick={() => setPage('lm')}>
            <Brain size={14} />
            LM Analysis
          </div>
        </div>
      </div>
      {page === 'dashboard' && <Dashboard />}
      {page === 'lm' && <LmAnalysis />}
      <Footer />
    </div>
  );
}
