import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import EmployeeList from './components/EmployeeList';
import EmployeeDetail from './components/EmployeeDetail';
import DepartmentList from './components/DepartmentList';
import OrgChart from './components/OrgChart';

function App() {
  return (
    <BrowserRouter>
      <div style={{ fontFamily: 'sans-serif', padding: '20px' }}>
        <header style={{
          borderBottom: '2px solid #333',
          paddingBottom: '10px',
          marginBottom: '20px'
        }}>
          <h1>ChronoStaff - HR Management System</h1>
          <nav style={{ display: 'flex', gap: '20px', marginTop: '10px' }}>
            <Link to="/">Employees</Link>
            <Link to="/departments">Departments</Link>
            <Link to="/org-chart">Organization Chart</Link>
          </nav>
        </header>

        <Routes>
          <Route path="/" element={<EmployeeList />} />
          <Route path="/employees/:id" element={<EmployeeDetail />} />
          <Route path="/departments" element={<DepartmentList />} />
          <Route path="/org-chart" element={<OrgChart />} />
        </Routes>
      </div>
    </BrowserRouter>
  );
}

export default App;
