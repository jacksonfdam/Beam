/** The Beam mark: a slide projecting a cone of light. Decorative by default. */
export function BeamMark({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      aria-hidden="true"
    >
      <rect x="3" y="6" width="8" height="12" rx="1.5" />
      <path d="M11 9l9-3v12l-9-3" opacity="0.85" />
    </svg>
  );
}
