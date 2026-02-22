import { useRef, useEffect } from "react";
import { useField } from "formik";
import Editor from "@monaco-editor/react";
import { makeStyles } from "@material-ui/styles";
import { useTheme } from "@material-ui/core/styles";
import { FormHelperText, InputLabel } from "@material-ui/core";
import clsx from "clsx";

const useStyles = makeStyles((theme) => ({
  wrapper: {
    width: "100%",
  },
  monaco: {
    padding: 10,
    width: "100%",
    borderColor: theme.palette.divider,
    borderStyle: "solid",
    borderWidth: 1,
    borderRadius: 4,
    backgroundColor: theme.palette.background.paper,
    "&:focus-within": {
      margin: -2,
      borderColor: theme.palette.primary.main,
      borderStyle: "solid",
      borderWidth: 2,
    },
  },
  label: {
    display: "block",
    marginBottom: 8,
  },
}));

export default function ({
  className,
  label,
  height,
  reinitialize = false,
  ...props
}) {
  const classes = useStyles();
  const theme = useTheme();
  const [field, meta, helper] = useField(props);
  const editorRef = useRef(null);

  function handleEditorMount(editor) {
    editorRef.current = editor;
    editor.onDidBlurEditorText(() => {
      helper.setValue(editorRef.current.getValue());
    });
  }

  useEffect(() => {
    if (reinitialize && editorRef.current) {
      editorRef.current.getModel().setValue(field.value);
    }
  }, [reinitialize, field.value]);

  return (
    <div className={clsx([classes.wrapper, className])}>
      <InputLabel variant="outlined" error={meta.touched && !!meta.error}>
        {label}
      </InputLabel>

      <Editor
        className={classes.monaco}
        height={height || 90}
        theme={theme.palette.type === "dark" ? "vs-dark" : "light"}
        defaultLanguage="json"
        onMount={handleEditorMount}
        defaultValue={field.value}
        options={{
          tabSize: 2,
          minimap: { enabled: false },
          lightbulb: { enabled: false },
          quickSuggestions: false,

          lineNumbers: "off",
          glyphMargin: false,
          folding: false,
          // Undocumented see https://github.com/Microsoft/vscode/issues/30795#issuecomment-410998882
          lineDecorationsWidth: 0,
          lineNumbersMinChars: 0,
          renderLineHighlight: "none",

          overviewRulerLanes: 0,
          hideCursorInOverviewRuler: true,
          scrollbar: {
            vertical: "hidden",
          },
          overviewRulerBorder: false,
        }}
      />

      {meta.touched && meta.error ? (
        <FormHelperText variant="outlined" error>
          {meta.error}
        </FormHelperText>
      ) : null}
    </div>
  );
}
