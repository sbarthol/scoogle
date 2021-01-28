import React from "react";
import "./Banner.css";

function Banner({ nResults, processingTimeMillis, selectedPage }) {
  return (
    <div className="BannerBox">
      Page {selectedPage} of {nResults} results ({processingTimeMillis / 1000}{" "}
      seconds)
    </div>
  );
}

export default Banner;
