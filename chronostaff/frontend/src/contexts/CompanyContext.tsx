import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';

interface Company {
  id: number;
  name: string;
}

interface CompanyContextType {
  selectedCompanyId: number | null;
  setSelectedCompanyId: (id: number) => void;
  companies: Company[];
  addCompany: (company: Company) => void;
  isLoading: boolean;
}

const CompanyContext = createContext<CompanyContextType | undefined>(undefined);

export function CompanyProvider({ children }: { children: ReactNode }) {
  const [selectedCompanyId, setSelectedCompanyId] = useState<number | null>(null);
  const [companies, setCompanies] = useState<Company[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Load companies from localStorage or default to company 1
  useEffect(() => {
    const savedCompanyId = localStorage.getItem('selectedCompanyId');
    const savedCompanies = localStorage.getItem('companies');

    if (savedCompanies) {
      const parsedCompanies = JSON.parse(savedCompanies);
      setCompanies(parsedCompanies);
    } else {
      // Default to company 1 (seed data company)
      const defaultCompanies = [{ id: 1, name: 'サンプル株式会社' }];
      setCompanies(defaultCompanies);
      localStorage.setItem('companies', JSON.stringify(defaultCompanies));
    }

    if (savedCompanyId) {
      setSelectedCompanyId(Number(savedCompanyId));
    } else {
      // Default to company 1 (seed data company)
      setSelectedCompanyId(1);
      localStorage.setItem('selectedCompanyId', '1');
    }

    setIsLoading(false);
  }, []);

  const handleSetSelectedCompanyId = (id: number) => {
    setSelectedCompanyId(id);
    localStorage.setItem('selectedCompanyId', String(id));
  };

  const handleAddCompany = (company: Company) => {
    const updatedCompanies = [...companies, company];
    setCompanies(updatedCompanies);
    localStorage.setItem('companies', JSON.stringify(updatedCompanies));
  };

  return (
    <CompanyContext.Provider
      value={{
        selectedCompanyId,
        setSelectedCompanyId: handleSetSelectedCompanyId,
        companies,
        addCompany: handleAddCompany,
        isLoading,
      }}
    >
      {children}
    </CompanyContext.Provider>
  );
}

export function useCompany() {
  const context = useContext(CompanyContext);
  if (context === undefined) {
    throw new Error('useCompany must be used within a CompanyProvider');
  }
  return context;
}
