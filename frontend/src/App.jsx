import React from 'react';

import logo from './images/scoogle_logo.png';
import SearchBox from './components/SearchBox';
import GButton from './components/GButton';
import SearchResults from './components/SearchResults';
import Header from './components/Header'

class App extends React.Component {

  constructor(props){

    super(props)
    this.state = {
      searchResults: [],
      showHomePage: true,
      searchBarText: ""
    }

    this.getSearchResults = this.getSearchResults.bind(this)
    this.handleSearchBarChange = this.handleSearchBarChange.bind(this)
  }

  handleSearchBarChange(e) {
    this.setState({searchBarText: e.target.value})
  }

  getSearchResults() {

    this.setState({searchResults: [], showHomePage: false})
    const apiUrl = `/api?query=${this.state.searchBarText}`;

    fetch(apiUrl)
      .then((res) => {return res.json()})
      .then((searchResults) => {
        console.log(searchResults)
        this.setState({ searchResults: searchResults, showHomePage: false });
      });
  }

  render() {

    if (this.state.showHomePage) {

      return (
        <div className="googleBox">
          <img src={logo} className="logo" alt="logo" />
          <SearchBox handleSearchBarChange={this.handleSearchBarChange} 
                        text={this.state.searchBarText} />
          <div className="buttonBox">
            <GButton text="Google Search" onClick={this.getSearchResults} />
            <GButton text="I'm Feeling Lucky" />
          </div>
        </div>
      );
    } else {

      return (
        <div>
          <Header searchBarText={this.state.searchBarText} 
                  handleSearchBarChange={this.handleSearchBarChange} />
          <SearchResults searchResults={this.state.searchResults} />
        </div>
      );
    }
  }
}

export default App;
