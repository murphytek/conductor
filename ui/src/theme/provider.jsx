import React, { useMemo } from "react";
import { MuiThemeProvider } from "@material-ui/core/styles";
import { createTheme } from "./theme";
import { useDarkMode } from "./DarkModeContext";

export const Provider = ({ children, ...rest }) => {
  const { isDarkMode } = useDarkMode();
  const theme = useMemo(
    () => createTheme(isDarkMode ? "dark" : "light"),
    [isDarkMode]
  );

  return (
    <MuiThemeProvider theme={theme} {...rest}>
      {children}
    </MuiThemeProvider>
  );
};
