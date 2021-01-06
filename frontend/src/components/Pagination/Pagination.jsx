import React from "react";
import "./Pagination.css";
import oBlack from "./images/o_blue.png";
import oRed from "./images/o_red.png";
import S from "./images/S.png";
import c from "./images/c.png";
import g from "./images/g.png";
import l from "./images/l.png";
import e from "./images/e.png";

function PaginationButton({ onClick, pageNumber, selected }) {
  return (
    <div className="OBox">
      <img className="OImg" alt="" src={selected ? oBlack : oRed}></img>
      <button
        className={selected ? "OBtnSelected" : "OBtn"}
        onClick={selected ? null : onClick}
      >
        {pageNumber}
      </button>
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
    <div className="PaginationOuterBox">
      <div className="PaginationInnerBox">
        {selectedO === 1 ? (
          <div className="PlaceHolderBtn"></div>
        ) : (
          <button
            className="PreviousBtn"
            onClick={() => getSearchResultsForPage(selectedO - 1)}
          >
            Previous
          </button>
        )}
        <img className="LetterBox" src={S} alt=""></img>
        <img className="LetterBox" src={c} alt=""></img>
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
        <img className="LetterBox" src={g} alt=""></img>
        <img className="LetterBox" src={l} alt=""></img>
        <img className="LetterBox" src={e} alt=""></img>
        {selectedO === numberOfOs ? (
          <div className="PlaceHolderBtn"></div>
        ) : (
          <button
            className="NextBtn"
            onClick={() => getSearchResultsForPage(selectedO + 1)}
          >
            Next
          </button>
        )}
      </div>
    </div>
  );
}

export default Pagination;
