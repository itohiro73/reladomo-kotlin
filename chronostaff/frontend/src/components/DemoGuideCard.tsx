import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

interface DemoGuideCardProps {
  step: number;
  totalSteps: number;
  title: string;
  description: string;
  objectives: string[];
  nextStep?: string;
  prevStep?: string;
  children?: React.ReactNode;
}

export default function DemoGuideCard({
  step,
  totalSteps,
  title,
  description,
  objectives,
  nextStep,
  prevStep,
  children
}: DemoGuideCardProps) {
  const navigate = useNavigate();

  // Scroll to top when component mounts
  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, []);

  return (
    <div className="space-y-6">
      {/* Progress Indicator */}
      <div className="bg-white rounded-lg shadow-md p-4">
        <div className="flex items-center justify-between mb-2">
          <span className="text-sm font-medium text-gray-600">
            デモガイド進捗
          </span>
          <span className="text-sm font-bold text-primary-600">
            Step {step} / {totalSteps}
          </span>
        </div>
        <div className="w-full bg-gray-200 rounded-full h-2">
          <div
            className="bg-primary-600 h-2 rounded-full transition-all duration-300"
            style={{ width: `${(step / totalSteps) * 100}%` }}
          />
        </div>
      </div>

      {/* Guide Card */}
      <div className="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-lg shadow-lg p-6 border-l-4 border-primary-600">
        <h2 className="text-2xl font-bold text-gray-900 mb-3 flex items-center gap-2">
          <span className="text-3xl">📝</span>
          {title}
        </h2>
        <p className="text-gray-700 mb-4">{description}</p>

        <div className="bg-white rounded-lg p-4 mb-4">
          <h3 className="font-semibold text-gray-900 mb-2 flex items-center gap-2">
            <span>🎯</span>
            このステップの目標
          </h3>
          <ul className="space-y-2">
            {objectives.map((objective, index) => (
              <li key={index} className="flex items-start gap-2 text-gray-700">
                <span className="text-green-500 mt-0.5">✓</span>
                <span>{objective}</span>
              </li>
            ))}
          </ul>
        </div>

        {/* Navigation Buttons */}
        <div className="flex gap-3">
          {prevStep && (
            <button
              onClick={() => navigate(prevStep)}
              className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors font-medium flex items-center gap-2"
            >
              <span>◀️</span>
              前のステップ
            </button>
          )}
          {nextStep && (
            <button
              onClick={() => navigate(nextStep)}
              className="flex-1 px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium flex items-center justify-center gap-2"
            >
              次のステップへ
              <span>▶️</span>
            </button>
          )}
        </div>
      </div>

      {/* Main Content */}
      <div>
        {children}
      </div>
    </div>
  );
}
