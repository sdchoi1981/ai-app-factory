export function applyTheme(root, preference) {
  if (preference === 'dark' || preference === 'light') root.dataset.theme = preference;
  else delete root.dataset.theme;
}

export function openDialog(dialog) {
  if (!dialog.open) dialog.showModal();
}
