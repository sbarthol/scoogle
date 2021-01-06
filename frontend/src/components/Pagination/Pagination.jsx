import React from "react";
import "./Pagination.css";

function PaginationButton({ onClick, pageNumber, selected }) {
  return (
    <div>
      <img alt="" src="o.png"></img>
      <button onClick={onClick}>{pageNumber}</button>
    </div>
  );
}

function Pagination({ getSearchResultsForPage, numberOfOs, selectedO }) {
  const firstPage = Math.max(
    1,
    selectedO - 5 - Math.max(0, 4 - (numberOfOs - selectedO))
  );

  const pages = [];
  for (var i = 0; i < 10 && firstPage + i <= numberOfOs; i++)
    pages.push(firstPage + i);

  return (
    <div>
      {selectedO !== 1 && (
        <button onClick={() => getSearchResultsForPage(selectedO - 1)}>
          Previous
        </button>
      )}
      {pages.map((page) => {
        return (
          <PaginationButton
            key={page}
            pageNumber={page}
            selected={page === selectedO}
            onClick={() => getSearchResultsForPage(page)}
          />
        );
      })}
      {numberOfOs !== selectedO && (
        <button onClick={() => getSearchResultsForPage(selectedO + 1)}>
          Next
        </button>
      )}
    </div>
  );
}

export default Pagination;
