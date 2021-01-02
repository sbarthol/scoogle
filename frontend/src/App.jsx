import React from 'react';

import logo from './images/scoogle_logo.png';
import SearchBox from './components/SearchBox';
import GButton from './components/GButton';


function App() {
  return (
    <div className="googleBox">
      <img src={logo} className="logo" alt="logo" />
      <SearchBox />
      <div className="buttonBox">
        <GButton text="Google Search" />
        <GButton text="I'm Feeling Lucky" />
      </div>
    </div>
  );
}

export default App;
