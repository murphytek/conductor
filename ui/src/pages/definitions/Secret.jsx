import React, { useMemo, useState } from "react";
import { NavLink, DataTable, Button } from "../../components";
import { makeStyles } from "@material-ui/styles";
import { MenuItem, TextField } from "@material-ui/core";
import Header from "./Header";
import sharedStyles from "../styles";
import { Helmet } from "react-helmet";
import AddIcon from "@material-ui/icons/Add";
import { useSecrets } from "../../data/secret";
import { useWorkflowNames } from "../../data/workflow";

const useStyles = makeStyles({
  ...sharedStyles,
  filterRow: {
    display: "flex",
    alignItems: "center",
    gap: 16,
    marginBottom: 16,
  },
  workflowFilter: {
    minWidth: 250,
  },
});

const columns = [
  {
    name: "name",
    renderer: (name, row) => {
      const path = row.workflowName
        ? `/secretDef/${name}?workflowName=${encodeURIComponent(row.workflowName)}`
        : `/secretDef/${name}`;
      return <NavLink path={path}>{name}</NavLink>;
    },
  },
];

export default function SecretDefinitions() {
  const classes = useStyles();
  const [workflowFilter, setWorkflowFilter] = useState("");
  const workflowNames = useWorkflowNames();

  const workflowName = workflowFilter || undefined;
  const { data: secrets, isFetching } = useSecrets(workflowName);

  const secretRows = useMemo(
    () =>
      Array.isArray(secrets)
        ? secrets.map((name) => ({ name, workflowName: workflowName }))
        : [],
    [secrets, workflowName]
  );

  const newSecretPath = workflowName
    ? `/secretDef?workflowName=${encodeURIComponent(workflowName)}`
    : "/secretDef";

  return (
    <div className={classes.wrapper}>
      <Helmet>
        <title>Conductor UI - Secret Definitions</title>
      </Helmet>

      <Header tabIndex={3} loading={isFetching} />

      <div className={classes.tabContent}>
        <div className={classes.filterRow}>
          <TextField
            className={classes.workflowFilter}
            select
            label="Scope"
            value={workflowFilter}
            onChange={(e) => setWorkflowFilter(e.target.value)}
            variant="outlined"
            size="small"
          >
            <MenuItem value="">Global Secrets</MenuItem>
            {workflowNames.map((name) => (
              <MenuItem key={name} value={name}>
                {name}
              </MenuItem>
            ))}
          </TextField>

          <Button component={NavLink} path={newSecretPath} startIcon={<AddIcon />}>
            New Secret
          </Button>
        </div>

        {secrets && (
          <DataTable
            title={`${secretRows.length} results`}
            localStorageKey="secretsTable"
            defaultShowColumns={["name"]}
            keyField="name"
            default
            data={secretRows}
            columns={columns}
          />
        )}
      </div>
    </div>
  );
}
