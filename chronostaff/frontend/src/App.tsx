import { BrowserRouter as Router, Routes, Route, Link, useLocation } from 'react-router-dom';
import { useState } from 'react';
import EmployeeList from './components/EmployeeList';
import EmployeeDetail from './components/EmployeeDetail';
import DepartmentList from './components/DepartmentList';
import PositionList from './components/PositionList';
import OrgChart from './components/OrgChart';
import InitialSetupWizard from './components/InitialSetupWizard';
import EmployeeAddForm from './components/EmployeeAddForm';
import ScheduledChangesView from './components/ScheduledChangesView';
import CompanySelector from './components/CompanySelector';
import { CompanyProvider } from './contexts/CompanyContext';

// Demo Step Components
import DemoStep1 from './components/DemoStep1';
import DemoStep2 from './components/DemoStep2';
import DemoStep3 from './components/DemoStep3';
import DemoStep4 from './components/DemoStep4';
import DemoStep5 from './components/DemoStep5';

function App() {
  return (
    <CompanyProvider>
      <Router>
        <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100">
          <Navigation />
          <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <Routes>
              {/* Demo Routes */}
              <Route path="/" element={<DemoStep1 />} />
              <Route path="/demo/step1" element={<DemoStep1 />} />
              <Route path="/demo/step2" element={<DemoStep2 />} />
              <Route path="/demo/step3" element={<DemoStep3 />} />
              <Route path="/demo/step4" element={<DemoStep4 />} />
              <Route path="/demo/step5" element={<DemoStep5 />} />

              {/* Management Routes */}
              <Route path="/employees" element={<EmployeeList />} />
              <Route path="/setup" element={<InitialSetupWizard />} />
              <Route path="/employees/new" element={<EmployeeAddForm />} />
              <Route path="/employees/:id" element={<EmployeeDetail />} />
              <Route path="/positions" element={<PositionList />} />
              <Route path="/departments" element={<DepartmentList />} />
              <Route path="/org-chart" element={<OrgChart />} />
              <Route path="/scheduled-changes" element={<ScheduledChangesView />} />
            </Routes>
          </main>
        </div>
      </Router>
    </CompanyProvider>
  );
}

function Navigation() {
  const location = useLocation();
  const [showDemoMenu, setShowDemoMenu] = useState(false);
  const [showAdminMenu, setShowAdminMenu] = useState(false);

  const isActive = (path: string) => location.pathname === path;
  const isDemoPath = location.pathname.startsWith('/demo') || location.pathname === '/';

  const demoItems = [
    { path: '/demo/step1', label: 'Step 1: 組織セットアップ', icon: '🎯' },
    { path: '/demo/step2', label: 'Step 2: 従業員雇用', icon: '➕' },
    { path: '/demo/step3', label: 'Step 3: 組織図確認', icon: '📊' },
    { path: '/demo/step4', label: 'Step 4: 未来の変更登録', icon: '🔮' },
    { path: '/demo/step5', label: 'Step 5: 時間旅行デモ', icon: '⏰' },
  ];

  const adminItems = [
    { path: '/employees', label: '従業員一覧', icon: '👥' },
    { path: '/setup', label: '初期セットアップ', icon: '🎯' },
    { path: '/employees/new', label: '従業員追加', icon: '➕' },
    { path: '/positions', label: '役職管理', icon: '👔' },
    { path: '/departments', label: '部署管理', icon: '🏢' },
    { path: '/org-chart', label: '組織図', icon: '📊' },
    { path: '/scheduled-changes', label: '予定変更一覧', icon: '🔮' },
  ];

  return (
    <header className="bg-gradient-to-r from-primary-600 to-primary-800 shadow-lg">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <div className="flex items-center space-x-6">
            <Link to="/" className="flex items-center gap-2">
              <h1 className="text-2xl font-bold text-white flex items-center gap-2 hover:text-primary-100 transition-colors">
                <span className="text-3xl">⏱️</span>
                ChronoStaff
              </h1>
            </Link>

            <nav className="hidden md:flex space-x-2">
              {/* Demo Guide Dropdown */}
              <div className="relative">
                <button
                  onClick={() => {
                    setShowDemoMenu(!showDemoMenu);
                    setShowAdminMenu(false);
                  }}
                  className={`px-4 py-2 rounded-md text-sm font-medium transition-colors duration-200 flex items-center gap-2 ${
                    isDemoPath
                      ? 'bg-primary-700 text-white shadow-md'
                      : 'text-primary-100 hover:bg-primary-700 hover:text-white'
                  }`}
                >
                  <span>📚</span>
                  デモガイド
                  <span className="text-xs">▼</span>
                </button>
                {showDemoMenu && (
                  <div className="absolute left-0 mt-2 w-64 bg-white rounded-md shadow-xl z-50 py-1">
                    {demoItems.map((item) => (
                      <Link
                        key={item.path}
                        to={item.path}
                        onClick={() => setShowDemoMenu(false)}
                        className={`block px-4 py-2 text-sm transition-colors flex items-center gap-2 ${
                          isActive(item.path)
                            ? 'bg-primary-50 text-primary-700 font-semibold'
                            : 'text-gray-700 hover:bg-gray-100'
                        }`}
                      >
                        <span>{item.icon}</span>
                        {item.label}
                      </Link>
                    ))}
                  </div>
                )}
              </div>

              {/* Admin Menu Dropdown */}
              <div className="relative">
                <button
                  onClick={() => {
                    setShowAdminMenu(!showAdminMenu);
                    setShowDemoMenu(false);
                  }}
                  className={`px-4 py-2 rounded-md text-sm font-medium transition-colors duration-200 flex items-center gap-2 ${
                    !isDemoPath
                      ? 'bg-primary-700 text-white shadow-md'
                      : 'text-primary-100 hover:bg-primary-700 hover:text-white'
                  }`}
                >
                  <span>⚙️</span>
                  管理
                  <span className="text-xs">▼</span>
                </button>
                {showAdminMenu && (
                  <div className="absolute left-0 mt-2 w-56 bg-white rounded-md shadow-xl z-50 py-1">
                    {adminItems.map((item) => (
                      <Link
                        key={item.path}
                        to={item.path}
                        onClick={() => setShowAdminMenu(false)}
                        className={`block px-4 py-2 text-sm transition-colors flex items-center gap-2 ${
                          isActive(item.path)
                            ? 'bg-primary-50 text-primary-700 font-semibold'
                            : 'text-gray-700 hover:bg-gray-100'
                        }`}
                      >
                        <span>{item.icon}</span>
                        {item.label}
                      </Link>
                    ))}
                  </div>
                )}
              </div>
            </nav>
          </div>

          <div className="flex items-center gap-3">
            <CompanySelector />
            <span className="text-primary-100 text-sm hidden lg:block">Bitemporal HR Management</span>
          </div>
        </div>
      </div>

      {/* Click outside to close dropdowns */}
      {(showDemoMenu || showAdminMenu) && (
        <div
          className="fixed inset-0 z-40"
          onClick={() => {
            setShowDemoMenu(false);
            setShowAdminMenu(false);
          }}
        />
      )}
    </header>
  );
}

export default App;
