import React from 'react';
import './SearchResults.css';

function SearchResult({title, link, text}) {

  return (
    <div className="Block">
      <p className="Link">{link}</p>
      <a href={link} className="Title">{title}</a>
      <p className="Text">{text}</p>
    </div>
  )
}

function SearchResults({searchResults}) {

  return (
    <div className="SearchResultsBox">
      {searchResults.map((searchResult => {
        return (
          <SearchResult 
            title = {searchResult.title} 
            link = {searchResult.link} 
            text = {searchResult.text} />
        )
      }))}
    </div>
  );
}

export default SearchResults;
