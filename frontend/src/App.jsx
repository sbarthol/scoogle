import React from "react";

import logo from "./images/scoogle_logo.png";
import SearchBox from "./components/SearchBox";
import Button from "./components/Button";
import SearchResults from "./components/SearchResults";
import Header from "./components/Header";
import Pagination from "./components/Pagination";
import NoResults from "./components/NoResults";
import Error from "./components/Error";

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
      loading: false,
      error: false,
      errorDescription: "",
      errorStatus: 0,
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

  feelLucky(query) {
    const apiUrl = `/api?query=${query}&pageNumber=1`;

    fetch(apiUrl)
      .then((res) => {
        return res.json();
      })
      .then((searchResults) => {
        if (searchResults.links.length >= 1) {
          window.location.replace(searchResults.links[0].link);
        }
      })
      .catch(console.log);
  }

  getSearchResults(query, pageNumber) {
    this.setState({
      links: [],
      totalPages: 1,
      showHomePage: false,
      loading: true,
      error: false,
    });
    const apiUrl = `/api?query=${query}&pageNumber=${pageNumber}`;

    fetch(apiUrl).then((res) => {
      if (res.status === 200) {
        res
          .json()
          .then((searchResults) => {
            this.setState({
              links: searchResults.links,
              totalPages: searchResults.totalPages,
              showHomePage: false,
              loading: false,
            });
          })
          .catch(console.log);
      } else {
        this.setState({
          showHomePage: false,
          loading: false,
          error: true,
          errorDescription: res.statusText,
          errorStatus: res.status,
        });
      }
    });
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
            <Button
              text="I'm Feeling Lucky"
              onClick={() => {
                if (this.state.searchBarText.replace(/\s/g, "").length) {
                  const searchText = this.state.searchBarText;
                  this.setState({ searchText: searchText });
                  this.feelLucky(searchText);
                }
              }}
            />
          </div>
        </div>
      );
    } else if (this.state.error) {
      return (
        <div>
          <Header
            searchBarText={this.state.searchBarText}
            handleSearchBarChange={this.handleSearchBarChange}
            onKeyDown={this.onKeyDown}
          />
          <Error
            status={this.state.errorStatus}
            description={this.state.errorDescription}
          />
        </div>
      );
    } else if (!this.state.loading && this.state.links.length > 0) {
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
    } else if (!this.state.loading) {
      return (
        <div>
          <Header
            searchBarText={this.state.searchBarText}
            handleSearchBarChange={this.handleSearchBarChange}
            onKeyDown={this.onKeyDown}
          />
          <NoResults searchText={this.state.searchText} />
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
        </div>
      );
    }
  }
}

export default App;
