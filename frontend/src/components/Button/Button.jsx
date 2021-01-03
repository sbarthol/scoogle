import React from "react";
import PropTypes from "prop-types";
import "./Button.css";

function Button(props) {
  const { text, onClick } = props;
  return (
    <button type="button" onClick={onClick}>
      {text}
    </button>
  );
}

Button.propTypes = {
  text: PropTypes.string.isRequired,
  onClick: PropTypes.func,
};

Button.defaultProps = {
  onClick: null,
};

export default Button;
