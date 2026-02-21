import { useState } from 'react';
import { Modal } from '@/shared/components/Modal';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Select } from '@/shared/components/Select';
import { useToastStore } from '@/stores/toast.store';
import { useCreateConnector, type ConnectorType } from '@/api/connectors.api';

interface ConnectorFormModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function ConnectorFormModal({ isOpen, onClose }: ConnectorFormModalProps) {
  const addToast = useToastStore((s) => s.addToast);
  const createConnector = useCreateConnector();

  const [name, setName] = useState('');
  const [type, setType] = useState<ConnectorType>('JDBC');
  const [description, setDescription] = useState('');

  // JDBC fields
  const [jdbcUrl, setJdbcUrl] = useState('');
  const [jdbcUsername, setJdbcUsername] = useState('');
  const [jdbcPassword, setJdbcPassword] = useState('');

  // HTTP fields
  const [httpBaseUrl, setHttpBaseUrl] = useState('');
  const [httpApiKey, setHttpApiKey] = useState('');

  // Redis fields
  const [redisHost, setRedisHost] = useState('');
  const [redisPort, setRedisPort] = useState('6379');

  const handleClose = () => {
    setName('');
    setType('JDBC');
    setDescription('');
    setJdbcUrl('');
    setJdbcUsername('');
    setJdbcPassword('');
    setHttpBaseUrl('');
    setHttpApiKey('');
    setRedisHost('');
    setRedisPort('6379');
    onClose();
  };

  const buildConfig = (): Record<string, unknown> => {
    switch (type) {
      case 'JDBC':
        return { url: jdbcUrl, username: jdbcUsername, password: jdbcPassword };
      case 'HTTP':
        return { baseUrl: httpBaseUrl, apiKey: httpApiKey || undefined };
      case 'REDIS':
        return { host: redisHost, port: parseInt(redisPort, 10) };
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    try {
      await createConnector.mutateAsync({
        name: name.trim(),
        type,
        description: description.trim() || undefined,
        config: buildConfig(),
      });
      addToast({ type: 'success', title: 'Connector created successfully' });
      handleClose();
    } catch {
      addToast({ type: 'error', title: 'Failed to create connector' });
    }
  };

  return (
    <Modal open={isOpen} onClose={handleClose} title="Add Connector">
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label htmlFor="conn-name" className="block text-sm font-medium text-gray-700 mb-1">
            Connector Name
          </label>
          <Input
            id="conn-name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g. Production Database"
            autoFocus
            required
          />
        </div>

        <div>
          <label htmlFor="conn-type" className="block text-sm font-medium text-gray-700 mb-1">
            Type
          </label>
          <Select
            id="conn-type"
            value={type}
            onChange={(e) => setType(e.target.value as ConnectorType)}
          >
            <option value="JDBC">JDBC (Database)</option>
            <option value="HTTP">HTTP (REST API)</option>
            <option value="REDIS">Redis</option>
          </Select>
        </div>

        <div>
          <label htmlFor="conn-desc" className="block text-sm font-medium text-gray-700 mb-1">
            Description (optional)
          </label>
          <Input
            id="conn-desc"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Brief description of this connector"
          />
        </div>

        {/* JDBC Config */}
        {type === 'JDBC' && (
          <>
            <div>
              <label htmlFor="jdbc-url" className="block text-sm font-medium text-gray-700 mb-1">
                JDBC URL
              </label>
              <Input
                id="jdbc-url"
                value={jdbcUrl}
                onChange={(e) => setJdbcUrl(e.target.value)}
                placeholder="jdbc:postgresql://localhost:5432/mydb"
                required
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label htmlFor="jdbc-user" className="block text-sm font-medium text-gray-700 mb-1">
                  Username
                </label>
                <Input
                  id="jdbc-user"
                  value={jdbcUsername}
                  onChange={(e) => setJdbcUsername(e.target.value)}
                  required
                />
              </div>
              <div>
                <label htmlFor="jdbc-pass" className="block text-sm font-medium text-gray-700 mb-1">
                  Password
                </label>
                <Input
                  id="jdbc-pass"
                  type="password"
                  value={jdbcPassword}
                  onChange={(e) => setJdbcPassword(e.target.value)}
                  required
                />
              </div>
            </div>
          </>
        )}

        {/* HTTP Config */}
        {type === 'HTTP' && (
          <>
            <div>
              <label htmlFor="http-url" className="block text-sm font-medium text-gray-700 mb-1">
                Base URL
              </label>
              <Input
                id="http-url"
                value={httpBaseUrl}
                onChange={(e) => setHttpBaseUrl(e.target.value)}
                placeholder="https://api.example.com"
                required
              />
            </div>
            <div>
              <label htmlFor="http-key" className="block text-sm font-medium text-gray-700 mb-1">
                API Key (optional)
              </label>
              <Input
                id="http-key"
                type="password"
                value={httpApiKey}
                onChange={(e) => setHttpApiKey(e.target.value)}
              />
            </div>
          </>
        )}

        {/* Redis Config */}
        {type === 'REDIS' && (
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label htmlFor="redis-host" className="block text-sm font-medium text-gray-700 mb-1">
                Host
              </label>
              <Input
                id="redis-host"
                value={redisHost}
                onChange={(e) => setRedisHost(e.target.value)}
                placeholder="localhost"
                required
              />
            </div>
            <div>
              <label htmlFor="redis-port" className="block text-sm font-medium text-gray-700 mb-1">
                Port
              </label>
              <Input
                id="redis-port"
                type="number"
                value={redisPort}
                onChange={(e) => setRedisPort(e.target.value)}
                required
              />
            </div>
          </div>
        )}

        <div className="flex justify-end gap-3 pt-2">
          <Button type="button" variant="outline" onClick={handleClose}>
            Cancel
          </Button>
          <Button
            type="submit"
            loading={createConnector.isPending}
            disabled={!name.trim()}
          >
            Create Connector
          </Button>
        </div>
      </form>
    </Modal>
  );
}
