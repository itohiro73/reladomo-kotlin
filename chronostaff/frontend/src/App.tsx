import { BrowserRouter as Router, Routes, Route, Link, useLocation } from 'react-router-dom';
import EmployeeList from './components/EmployeeList';
import EmployeeDetail from './components/EmployeeDetail';
import DepartmentList from './components/DepartmentList';
import OrgChart from './components/OrgChart';
import InitialSetupWizard from './components/InitialSetupWizard';
import EmployeeAddForm from './components/EmployeeAddForm';
import CompanySelector from './components/CompanySelector';
import { CompanyProvider } from './contexts/CompanyContext';

function App() {
  return (
    <CompanyProvider>
      <Router>
        <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100">
          <Navigation />
          <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <Routes>
              <Route path="/" element={<EmployeeList />} />
              <Route path="/setup" element={<InitialSetupWizard />} />
              <Route path="/employees/new" element={<EmployeeAddForm />} />
              <Route path="/employees/:id" element={<EmployeeDetail />} />
              <Route path="/departments" element={<DepartmentList />} />
              <Route path="/org-chart" element={<OrgChart />} />
            </Routes>
          </main>
        </div>
      </Router>
    </CompanyProvider>
  );
}

function Navigation() {
  const location = useLocation();

  const isActive = (path: string) => {
    return location.pathname === path;
  };

  const navItems = [
    { path: '/', label: '従業員一覧', icon: '👥' },
    { path: '/setup', label: '初期セットアップ', icon: '🎯' },
    { path: '/employees/new', label: '従業員追加', icon: '➕' },
    { path: '/departments', label: '部署一覧', icon: '🏢' },
    { path: '/org-chart', label: '組織図', icon: '📊' },
  ];

  return (
    <header className="bg-gradient-to-r from-primary-600 to-primary-800 shadow-lg">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <div className="flex items-center space-x-8">
            <h1 className="text-2xl font-bold text-white flex items-center gap-2">
              <span className="text-3xl">⏱️</span>
              ChronoStaff
            </h1>
            <nav className="hidden md:flex space-x-4">
              {navItems.map((item) => (
                <Link
                  key={item.path}
                  to={item.path}
                  className={`px-3 py-2 rounded-md text-sm font-medium transition-colors duration-200 flex items-center gap-2 ${
                    isActive(item.path)
                      ? 'bg-primary-700 text-white shadow-md'
                      : 'text-primary-100 hover:bg-primary-700 hover:text-white'
                  }`}
                >
                  <span>{item.icon}</span>
                  {item.label}
                </Link>
              ))}
            </nav>
          </div>
          <div className="flex items-center gap-3">
            <CompanySelector />
            <span className="text-primary-100 text-sm">Bitemporal HR Management</span>
          </div>
        </div>
      </div>
    </header>
  );
}

export default App;
