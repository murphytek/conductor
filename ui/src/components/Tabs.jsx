import React from "react";
import { Tabs as RawTabs, Tab as RawTab } from "@material-ui/core";
import { makeStyles } from "@material-ui/styles";

// Override styles for 'Contextual' tabs
const useContextualTabStyles = makeStyles((theme) => ({
  root: {
    color: theme.palette.text.secondary,
    textTransform: "none",
    height: 38,
    minHeight: 38,
    padding: "12px 16px",
    backgroundColor: theme.palette.grey[100],
    [theme.breakpoints.up("md")]: {
      minWidth: 0,
    },
    width: "auto",
    "&:hover": {
      backgroundColor: theme.palette.action.hover,
      color: theme.palette.text.secondary,
    },
  },
  selected: {
    backgroundColor: theme.palette.background.paper,
    color: theme.palette.text.primary,
    "&:hover": {
      backgroundColor: theme.palette.background.paper,
      color: theme.palette.text.primary,
    },
  },
  wrapper: {
    width: "auto",
  },
}));

const useContextualTabsStyles = makeStyles((theme) => ({
  indicator: {
    height: 0,
  },
  flexContainer: {
    backgroundColor: theme.palette.grey[100],
  },
}));

export default function Tabs({ contextual, children, ...props }) {
  const classes = useContextualTabsStyles();
  return (
    <RawTabs
      classes={contextual ? classes : null}
      indicatorColor="primary"
      {...props}
    >
      {contextual
        ? children.map((child, idx) =>
            React.cloneElement(child, { contextual: true, key: idx })
          )
        : children}
    </RawTabs>
  );
}

export function Tab({ contextual, ...props }) {
  const classes = useContextualTabStyles();
  return <RawTab classes={contextual ? classes : null} {...props} />;
}
