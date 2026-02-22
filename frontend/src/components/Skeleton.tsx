interface SkeletonProps {
  width?: string;
  height?: string;
  variant?: 'text' | 'circle' | 'rect';
  className?: string;
}

export function Skeleton({
  width,
  height,
  variant = 'text',
  className = '',
}: SkeletonProps) {
  const base = 'animate-pulse bg-gray-200';

  const variantClass =
    variant === 'circle'
      ? 'rounded-full'
      : variant === 'rect'
        ? 'rounded-md'
        : 'rounded';

  const defaultHeight = variant === 'text' ? 'h-4' : 'h-8';

  return (
    <div
      className={`${base} ${variantClass} ${className}`}
      style={{
        width: width ?? '100%',
        height: height ?? undefined,
      }}
    >
      {!height && <div className={defaultHeight} />}
    </div>
  );
}

export function SkeletonRow({ columns = 5 }: { columns?: number }) {
  return (
    <div className="flex items-center gap-4 px-6 py-3 border-b border-gray-100">
      {Array.from({ length: columns }).map((_, i) => (
        <Skeleton
          key={i}
          variant="text"
          width={i === 0 ? '40%' : '15%'}
          height="16px"
        />
      ))}
    </div>
  );
}

export function SkeletonCard() {
  return (
    <div className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
      <Skeleton variant="text" width="60%" height="20px" />
      <div className="mt-3">
        <Skeleton variant="text" width="40%" height="32px" />
      </div>
      <div className="mt-2">
        <Skeleton variant="text" width="50%" height="14px" />
      </div>
    </div>
  );
}
