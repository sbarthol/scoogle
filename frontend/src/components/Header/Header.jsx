import React from 'react';
import './Header.css';
import logo from '../../images/scoogle_logo.png';
import SearchBox from '../SearchBox';

function Header({searchBarText, handleSearchBarChange}) {
  return (
    <div className="HeaderBox">
      <img src={logo} className="Logo" alt="logo" />
      <div className="SearchBoxContainer">
        <SearchBox text={searchBarText} handleSearchBarChange={handleSearchBarChange} />
      </div>
    </div>
  );
}

export default Header;
