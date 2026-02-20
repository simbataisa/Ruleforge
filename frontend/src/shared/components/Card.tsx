import { cn } from '@/shared/utils/cn';
import type { ReactNode } from 'react';

interface CardProps {
  children: ReactNode;
  header?: ReactNode;
  noPadding?: boolean;
  className?: string;
}

export function Card({ children, header, noPadding, className }: CardProps) {
  return (
    <div
      className={cn(
        'rounded-lg border border-gray-200 bg-white shadow-sm',
        className,
      )}
    >
      {header && (
        <div className="border-b border-gray-200 px-6 py-4">{header}</div>
      )}
      <div className={cn(!noPadding && 'p-6')}>{children}</div>
    </div>
  );
}
