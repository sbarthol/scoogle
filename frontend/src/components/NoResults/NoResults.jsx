import React from "react";
import "./NoResults.css";

function NoResults({ searchText }) {
  return (
    <div className="NoResultsBox">
      <p>
        Your search - <em>{searchText}</em> - did not match any documents.
      </p>
      <p>Suggestions</p>
      <ul>
        <li>Make sure that all words are spelled correctly.</li>
        <li>Try different keywords.</li>
        <li>Try more general keywords.</li>
        <li>Try fewer keywords.</li>
      </ul>
    </div>
  );
}

export default NoResults;
