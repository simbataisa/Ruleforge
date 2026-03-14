import { useNavigate, useParams, Link } from 'react-router-dom';
import { ArrowLeft, Settings, BookOpen, Calendar, ChevronRight } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { Badge } from '@/shared/components/Badge';
import { Spinner } from '@/shared/components/Spinner';
import { useSpace } from '@/api/org.api';

export function SpaceDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: space, isLoading, isError } = useSpace(id ?? '');

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Spinner size="lg" />
      </div>
    );
  }

  if (isError || !space) {
    return (
      <div className="flex flex-col items-center justify-center h-64 gap-4">
        <p className="text-gray-500">Space not found or you don't have access.</p>
        <Button variant="outline" onClick={() => navigate('/spaces')}>
          Back to Spaces
        </Button>
      </div>
    );
  }

  const createdDate = new Date(space.createdAt).toLocaleDateString('en-US', {
    month: 'long',
    day: 'numeric',
    year: 'numeric',
  });

  return (
    <div className="flex flex-col h-full">
      {/* Breadcrumb */}
      <div className="flex items-center gap-1.5 px-6 pt-4 text-sm text-gray-500">
        <Link to="/spaces" className="hover:text-gray-700 transition-colors">
          Spaces
        </Link>
        <ChevronRight className="h-3.5 w-3.5" />
        <span className="text-gray-900 font-medium">{space.name}</span>
      </div>

      {/* Header */}
      <div className="flex items-start justify-between px-6 py-4 border-b border-gray-200">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/spaces')}
            className="p-1.5 rounded-md text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
            aria-label="Back to spaces"
          >
            <ArrowLeft className="h-4 w-4" />
          </button>
          <div>
            <h1 className="text-xl font-semibold text-gray-900">{space.name}</h1>
            <p className="text-sm text-gray-400 font-mono">{space.slug}</p>
          </div>
        </div>
        <Button variant="outline" size="sm" onClick={() => navigate(`/spaces/${id}/settings`)}>
          <Settings className="h-4 w-4" />
          Settings
        </Button>
      </div>

      {/* Stats Bar */}
      <div className="flex items-center gap-6 px-6 py-3 bg-gray-50 border-b border-gray-200 text-sm">
        <div className="flex items-center gap-2 text-gray-600">
          <BookOpen className="h-4 w-4 text-gray-400" />
          <span>
            <span className="font-semibold text-gray-900">{space.ruleCount}</span>{' '}
            rule{space.ruleCount !== 1 ? 's' : ''}
          </span>
        </div>
        <div className="flex items-center gap-2 text-gray-600">
          <Calendar className="h-4 w-4 text-gray-400" />
          <span>Created {createdDate}</span>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto p-6">
        {space.description && (
          <p className="text-sm text-gray-600 mb-6 max-w-2xl">{space.description}</p>
        )}

        {/* Overview Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
          <div className="bg-white border border-gray-200 rounded-lg p-5">
            <p className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">
              Total Rules
            </p>
            <p className="text-3xl font-bold text-gray-900">{space.ruleCount}</p>
          </div>
          <div className="bg-white border border-gray-200 rounded-lg p-5">
            <p className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Slug</p>
            <p className="text-sm font-mono text-gray-700 mt-2 truncate">{space.slug}</p>
          </div>
          <div className="bg-white border border-gray-200 rounded-lg p-5">
            <p className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">
              Status
            </p>
            <Badge className="bg-green-100 text-green-700 mt-2">Active</Badge>
          </div>
        </div>

        {/* Quick Actions */}
        <div className="flex gap-3">
          <Button onClick={() => navigate('/rules')}>
            View Rules
          </Button>
          <Button variant="outline" onClick={() => navigate(`/spaces/${id}/settings`)}>
            <Settings className="h-4 w-4" />
            Configure Space
          </Button>
        </div>
      </div>
    </div>
  );
}
