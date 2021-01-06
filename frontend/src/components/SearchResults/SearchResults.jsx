import React from "react";
import "./SearchResults.css";

function SearchResult({ title, link, text, cleanLink }) {
  return (
    <div className="Block">
      <p className="Link">{cleanLink}</p>
      <a href={link} className="Title">
        {title}
      </a>
      <p className="Text">{text}</p>
    </div>
  );
}

function SearchResults({ links }) {
  return (
    <div className="SearchResultsBox">
      {links.map((links) => {
        return (
          <SearchResult
            title={links.title}
            link={links.link}
            text={links.text}
            cleanLink={links.cleanLink}
          />
        );
      })}
    </div>
  );
}

export default SearchResults;
