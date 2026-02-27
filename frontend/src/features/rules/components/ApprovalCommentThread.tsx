import { useState } from 'react';
import { MessageSquare, Reply, Send } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { Spinner } from '@/shared/components/Spinner';
import {
  useApprovalComments,
  useAddApprovalComment,
  type ApprovalComment,
} from '@/api/rules.api';
import { useToastStore } from '@/stores/toast.store';

interface ApprovalCommentThreadProps {
  ruleId: string;
  version: number;
}

function formatTimeAgo(dateStr: string): string {
  const now = Date.now();
  const then = new Date(dateStr).getTime();
  const diffMs = now - then;
  const seconds = Math.floor(diffMs / 1000);
  if (seconds < 60) return 'just now';
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

function CommentItem({
  comment,
  replies,
  onReply,
}: {
  comment: ApprovalComment;
  replies: ApprovalComment[];
  onReply: (parentId: string) => void;
}) {
  return (
    <div className="space-y-3">
      <div className="flex gap-3">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-sm font-medium text-indigo-700">
          {comment.userName?.charAt(0).toUpperCase() || 'U'}
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <span className="text-sm font-medium text-gray-900">
              {comment.userName}
            </span>
            <span className="text-xs text-gray-400">
              {formatTimeAgo(comment.createdAt)}
            </span>
          </div>
          <p className="mt-1 text-sm text-gray-700 whitespace-pre-wrap">
            {comment.body}
          </p>
          <button
            onClick={() => onReply(comment.id)}
            className="mt-1 flex items-center gap-1 text-xs text-gray-400 hover:text-indigo-600"
          >
            <Reply className="h-3 w-3" />
            Reply
          </button>
        </div>
      </div>

      {replies.length > 0 && (
        <div className="ml-11 space-y-3 border-l-2 border-gray-100 pl-4">
          {replies.map((reply) => (
            <div key={reply.id} className="flex gap-3">
              <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-gray-100 text-xs font-medium text-gray-600">
                {reply.userName?.charAt(0).toUpperCase() || 'U'}
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <span className="text-sm font-medium text-gray-900">
                    {reply.userName}
                  </span>
                  <span className="text-xs text-gray-400">
                    {formatTimeAgo(reply.createdAt)}
                  </span>
                </div>
                <p className="mt-1 text-sm text-gray-700 whitespace-pre-wrap">
                  {reply.body}
                </p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export function ApprovalCommentThread({ ruleId, version }: ApprovalCommentThreadProps) {
  const addToast = useToastStore((s) => s.addToast);
  const { data: comments, isLoading } = useApprovalComments(ruleId, version);
  const addComment = useAddApprovalComment();
  const [body, setBody] = useState('');
  const [replyTo, setReplyTo] = useState<string | null>(null);

  const handleSubmit = () => {
    if (!body.trim()) return;
    addComment.mutate(
      { ruleId, version, body: body.trim(), parentId: replyTo ?? undefined },
      {
        onSuccess: () => {
          setBody('');
          setReplyTo(null);
        },
        onError: () => {
          addToast({ type: 'error', title: 'Failed to post comment' });
        },
      },
    );
  };

  const topLevelComments = (comments ?? []).filter((c) => !c.parentId);
  const repliesByParent = (comments ?? []).reduce<Record<string, ApprovalComment[]>>(
    (acc, c) => {
      if (c.parentId) {
        (acc[c.parentId] ??= []).push(c);
      }
      return acc;
    },
    {},
  );

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8">
        <Spinner />
      </div>
    );
  }

  return (
    <div>
      <h4 className="flex items-center gap-2 text-sm font-semibold text-gray-900 mb-4">
        <MessageSquare className="h-4 w-4" />
        Comments ({comments?.length ?? 0})
      </h4>

      {topLevelComments.length === 0 ? (
        <p className="text-sm text-gray-400 mb-4">No comments yet.</p>
      ) : (
        <div className="space-y-4 mb-4">
          {topLevelComments.map((comment) => (
            <CommentItem
              key={comment.id}
              comment={comment}
              replies={repliesByParent[comment.id] ?? []}
              onReply={setReplyTo}
            />
          ))}
        </div>
      )}

      {/* New comment form */}
      <div className="border-t border-gray-100 pt-4">
        {replyTo && (
          <div className="mb-2 flex items-center gap-2 text-xs text-gray-500">
            <Reply className="h-3 w-3" />
            Replying to comment
            <button
              onClick={() => setReplyTo(null)}
              className="text-indigo-600 hover:underline"
            >
              Cancel
            </button>
          </div>
        )}
        <div className="flex gap-2">
          <textarea
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder="Add a comment..."
            rows={2}
            className="flex-1 rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm placeholder:text-gray-400 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
            onKeyDown={(e) => {
              if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) {
                handleSubmit();
              }
            }}
          />
          <Button
            size="sm"
            onClick={handleSubmit}
            disabled={!body.trim()}
            loading={addComment.isPending}
          >
            <Send className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </div>
  );
}
