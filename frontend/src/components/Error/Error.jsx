import React from "react";
import "./Error.css";

function Error({ status, description }) {
  return (
    <div className="ErrorBox">
      <p>
        {status === -1 ? "" : <em>{status}.</em>}
        {description}.
      </p>
    </div>
  );
}

export default Error;
