import { useCompany } from '../contexts/CompanyContext';

export default function CompanySelector() {
  const { selectedCompanyId, setSelectedCompanyId, companies, isLoading } = useCompany();

  if (isLoading || companies.length === 0) {
    return null;
  }

  const selectedCompany = companies.find(c => c.id === selectedCompanyId);

  // If only one company, just display the name
  if (companies.length === 1) {
    return (
      <div className="flex items-center gap-2 px-3 py-1.5 bg-primary-700 rounded-md">
        <span className="text-lg">🏢</span>
        <span className="text-white text-sm font-medium">
          {selectedCompany?.name || '会社を選択'}
        </span>
      </div>
    );
  }

  // If multiple companies, show dropdown
  return (
    <div className="relative">
      <select
        value={selectedCompanyId || ''}
        onChange={(e) => setSelectedCompanyId(Number(e.target.value))}
        className="appearance-none bg-primary-700 text-white text-sm font-medium pl-9 pr-8 py-1.5 rounded-md cursor-pointer hover:bg-primary-600 transition-colors focus:outline-none focus:ring-2 focus:ring-white focus:ring-opacity-50"
      >
        <option value="" disabled>会社を選択</option>
        {companies.map((company) => (
          <option key={company.id} value={company.id}>
            {company.name}
          </option>
        ))}
      </select>
      <span className="absolute left-2 top-1/2 -translate-y-1/2 text-lg pointer-events-none">
        🏢
      </span>
      <span className="absolute right-2 top-1/2 -translate-y-1/2 text-white pointer-events-none">
        ▼
      </span>
    </div>
  );
}
