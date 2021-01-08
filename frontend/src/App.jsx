import React from "react";

import logo from "./images/scoogle_logo.png";
import SearchBox from "./components/SearchBox";
import Button from "./components/Button";
import SearchResults from "./components/SearchResults";
import Header from "./components/Header";
import Pagination from "./components/Pagination";

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      links: [],
      totalPages: 1,
      showHomePage: true,
      searchBarText: "",
      searchText: "",
      selectedPage: 1,
    };

    this.getSearchResults = this.getSearchResults.bind(this);
    this.handleSearchBarChange = this.handleSearchBarChange.bind(this);
    this.onKeyDown = this.onKeyDown.bind(this);
  }

  onKeyDown(e) {
    if (e.key === "Enter") {
      if (this.state.searchBarText.replace(/\s/g, "").length) {
        const searchText = this.state.searchBarText;
        this.setState({ searchText: searchText, selectedPage: 1 });
        this.getSearchResults(searchText, 1);
      }
    }
  }

  handleSearchBarChange(e) {
    this.setState({ searchBarText: e.target.value });
  }

  getSearchResults(query, pageNumber) {
    this.setState({
      links: [],
      totalPages: 1,
      showHomePage: false,
    });
    const apiUrl = `/api?query=${query}&pageNumber=${pageNumber}`;

    fetch(apiUrl)
      .then((res) => {
        return res.json();
      })
      .then((searchResults) => {
        this.setState({
          links: searchResults.links,
          totalPages: searchResults.totalPages,
          showHomePage: false,
        });
      })
      .catch(console.log);
  }

  render() {
    if (this.state.showHomePage) {
      return (
        <div className="googleBox">
          <img src={logo} className="logo" alt="logo" />
          <SearchBox
            handleSearchBarChange={this.handleSearchBarChange}
            text={this.state.searchBarText}
            onKeyDown={this.onKeyDown}
          />
          <div className="buttonBox">
            <Button
              text="Scoogle Search"
              onClick={() => {
                if (this.state.searchBarText.replace(/\s/g, "").length) {
                  const searchText = this.state.searchBarText;
                  this.setState({ searchText: searchText });
                  this.getSearchResults(searchText, 1);
                }
              }}
            />
            <Button text="I'm Feeling Lucky" />
          </div>
        </div>
      );
    } else {
      return (
        <div>
          <Header
            searchBarText={this.state.searchBarText}
            handleSearchBarChange={this.handleSearchBarChange}
            onKeyDown={this.onKeyDown}
          />
          <SearchResults links={this.state.links} />
          {this.state.totalPages >= 2 && (
            <Pagination
              numberOfOs={this.state.totalPages}
              selectedO={this.state.selectedPage}
              getSearchResultsForPage={(p) => {
                this.setState({ selectedPage: p });
                this.getSearchResults(this.state.searchText, p);
              }}
            />
          )}
        </div>
      );
    }
  }
}

export default App;
