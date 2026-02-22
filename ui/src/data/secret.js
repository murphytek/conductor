import { useMutation } from "react-query";
import { useFetchContext, fetchWithContext } from "../plugins/fetch";
import { useFetch } from "./common";

function buildPath(base, workflowName) {
  if (workflowName) {
    return `${base}?workflowName=${encodeURIComponent(workflowName)}`;
  }
  return base;
}

export function useSecrets(workflowName) {
  const path = buildPath("/secrets", workflowName);
  return useFetch(["secrets", workflowName || "global"], path);
}

export function useSecret(name, workflowName) {
  let path;
  if (name) {
    path = buildPath(`/secrets/${name}`, workflowName);
  }
  return useFetch(["secret", name, workflowName || "global"], path);
}

export function useSaveSecret(callbacks) {
  const fetchContext = useFetchContext();

  return useMutation(({ name, value, workflowName }) => {
    const path = buildPath(`/secrets/${name}`, workflowName);
    return fetchWithContext(path, fetchContext, {
      method: "put",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ value }),
    });
  }, callbacks);
}

export function useDeleteSecret(callbacks) {
  const fetchContext = useFetchContext();

  return useMutation(({ name, workflowName }) => {
    const path = buildPath(`/secrets/${name}`, workflowName);
    return fetchWithContext(path, fetchContext, {
      method: "delete",
    });
  }, callbacks);
}
