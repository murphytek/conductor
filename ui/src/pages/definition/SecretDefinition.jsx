import React, { useState, useEffect, useMemo } from "react";
import { useRouteMatch, useLocation } from "react-router-dom";
import { makeStyles } from "@material-ui/styles";
import { Helmet } from "react-helmet";
import {
  Toolbar,
  TextField,
  Paper,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  MenuItem,
} from "@material-ui/core";
import { Button, LinearProgress, Pill, Text } from "../../components";
import { usePushHistory } from "../../components/NavLink";
import { useSecret, useSaveSecret, useDeleteSecret } from "../../data/secret";
import { useWorkflowNames } from "../../data/workflow";
import _ from "lodash";

const useStyles = makeStyles({
  wrapper: {
    display: "flex",
    height: "100%",
    alignItems: "stretch",
    flexDirection: "column",
  },
  name: {
    fontWeight: "bold",
  },
  rightButtons: {
    display: "flex",
    flexGrow: 1,
    justifyContent: "flex-end",
    gap: 8,
  },
  form: {
    padding: 30,
    maxWidth: 600,
  },
  field: {
    marginBottom: 20,
  },
});

function useQueryParam(name) {
  const { search } = useLocation();
  return useMemo(() => new URLSearchParams(search).get(name), [search, name]);
}

export default function SecretDefinition() {
  const classes = useStyles();
  const match = useRouteMatch();
  const navigate = usePushHistory();
  const workflowNames = useWorkflowNames();

  const secretName = _.get(match, "params.name");
  const isNew = !secretName;
  const queryWorkflowName = useQueryParam("workflowName");

  const { isFetching } = useSecret(secretName, queryWorkflowName || undefined);

  const [name, setName] = useState("");
  const [value, setValue] = useState("");
  const [workflowName, setWorkflowName] = useState(queryWorkflowName || "");
  const [isModified, setIsModified] = useState(false);
  const [deleteDialog, setDeleteDialog] = useState(false);
  const [errorMsg, setErrorMsg] = useState(null);

  useEffect(() => {
    if (secretName) {
      setName(secretName);
    }
  }, [secretName]);

  useEffect(() => {
    if (queryWorkflowName) {
      setWorkflowName(queryWorkflowName);
    }
  }, [queryWorkflowName]);

  const { mutate: saveSecret, isLoading: isSaving } = useSaveSecret({
    onSuccess: () => {
      navigate("/secretDefs");
    },
    onError: (err) => {
      setErrorMsg(typeof err === "string" ? err : "Save failed");
    },
  });

  const { mutate: deleteSecret, isLoading: isDeleting } = useDeleteSecret({
    onSuccess: () => {
      navigate("/secretDefs");
    },
    onError: (err) => {
      setErrorMsg(typeof err === "string" ? err : "Delete failed");
    },
  });

  const handleSave = () => {
    if (!name.trim()) {
      setErrorMsg("Name is required");
      return;
    }
    if (!value.trim()) {
      setErrorMsg("Value is required");
      return;
    }
    setErrorMsg(null);
    saveSecret({
      name: name.trim(),
      value: value.trim(),
      workflowName: workflowName || undefined,
    });
  };

  const handleDelete = () => {
    setDeleteDialog(false);
    deleteSecret({ name: secretName, workflowName: workflowName || undefined });
  };

  const handleNameChange = (e) => {
    setName(e.target.value);
    setIsModified(true);
  };

  const handleValueChange = (e) => {
    setValue(e.target.value);
    setIsModified(true);
  };

  const handleWorkflowChange = (e) => {
    setWorkflowName(e.target.value);
    setIsModified(true);
  };

  const scopeLabel = workflowName ? `Workflow: ${workflowName}` : "Global";

  return (
    <>
      <Helmet>
        <title>
          Conductor UI - Secret Definition - {secretName || "New Secret"}
        </title>
      </Helmet>

      <Dialog
        fullWidth
        maxWidth="sm"
        open={deleteDialog}
        onClose={() => setDeleteDialog(false)}
      >
        <DialogTitle>Confirm Delete</DialogTitle>
        <DialogContent>
          <Text>
            Are you sure you want to delete secret "{secretName}" ({scopeLabel})?
            This action cannot be undone.
          </Text>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleDelete}>Confirm</Button>
          <Button variant="secondary" onClick={() => setDeleteDialog(false)}>
            Cancel
          </Button>
        </DialogActions>
      </Dialog>

      {(isFetching || isSaving || isDeleting) && <LinearProgress />}

      <div className={classes.wrapper}>
        <Toolbar>
          <Text className={classes.name}>{secretName || "NEW"}</Text>
          {isModified && <Pill color="yellow" label="Modified" />}

          <div className={classes.rightButtons}>
            <Button disabled={!isModified || isSaving} onClick={handleSave}>
              Save
            </Button>
            {!isNew && (
              <Button
                variant="secondary"
                disabled={isDeleting}
                onClick={() => setDeleteDialog(true)}
              >
                Delete
              </Button>
            )}
          </div>
        </Toolbar>

        {errorMsg && (
          <div style={{ padding: "0 30px" }}>
            <Text style={{ color: "red" }}>{errorMsg}</Text>
          </div>
        )}

        <Paper className={classes.form} elevation={0}>
          <TextField
            className={classes.field}
            fullWidth
            label="Name"
            value={name}
            onChange={handleNameChange}
            disabled={!isNew}
            variant="outlined"
          />
          <TextField
            className={classes.field}
            fullWidth
            select
            label="Workflow Scope"
            value={workflowName}
            onChange={handleWorkflowChange}
            disabled={!isNew}
            variant="outlined"
          >
            <MenuItem value="">Global</MenuItem>
            {workflowNames.map((wfName) => (
              <MenuItem key={wfName} value={wfName}>
                {wfName}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            className={classes.field}
            fullWidth
            label="Value"
            value={value}
            onChange={handleValueChange}
            type="password"
            variant="outlined"
            placeholder={isNew ? "" : "Enter new value to update"}
          />
        </Paper>
      </div>
    </>
  );
}
