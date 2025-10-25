import './App.css';
import { CategoryList } from './components/CategoryList';
import { ProductList } from './components/ProductList';
import { ProductPriceTimeline } from './components/ProductPriceTimeline';
import { DatabaseViewer } from './components/DatabaseViewer';

function App() {
  return (
    <div className="app">
      <header className="app-header">
        <h1>Reladomo Kotlin Demo</h1>
        <p>バイテンポラルデータモデルのデモンストレーション</p>
      </header>

      <main>
        <CategoryList />
        <ProductList />
        <ProductPriceTimeline />
        <DatabaseViewer />
      </main>

      <footer style={{
        textAlign: 'center',
        marginTop: '3rem',
        paddingTop: '2rem',
        borderTop: '1px solid #333',
        color: '#666'
      }}>
        <p>Reladomo Kotlin Wrapper - Conference Demo</p>
      </footer>
    </div>
  );
}

export default App;
