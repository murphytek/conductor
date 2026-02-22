export default (theme) => ({
  wrapper: {
    overflowY: "scroll",
    overflowX: "hidden",
    height: "100%",
  },
  padded: {
    padding: 30,
  },
  header: {
    backgroundColor: theme.palette.background.default,
    padding: "20px 30px 0 30px",
    zIndex: 1,
  },
  paddingBottom: {
    paddingBottom: 25,
  },
  tabContent: {
    padding: 30,
  },
  buttonRow: {
    marginBottom: 15,
    display: "flex",
    justifyContent: "flex-end",
  },
  field: {
    marginBottom: 15,
  },
});
