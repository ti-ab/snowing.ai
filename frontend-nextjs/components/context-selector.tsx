import { useState } from 'react';
import { DataPacket_Kind, Room } from 'livekit-client';

// par exemple, dans un React component :
function ContextSelector({ room }: { room: Room }) {
  const [ctxKey, setCtxKey] = useState<'anglais'|'python'>('anglais');

  const sendContext = (key: string) => {
    const payload = JSON.stringify({ type: 'setContext', context: key });
    room.localParticipant.publishData(
      new TextEncoder().encode(payload),
      DataPacket_Kind.RELIABLE
    );
  };

  return (
    <select
      value={ctxKey}
      onChange={e => {
        setCtxKey(e.target.value);
        sendContext(e.target.value);
      }}
    >
      <option value="anglais">Leçon d’anglais 60</option>
      <option value="python">Leçon de Python</option>
    </select>
  );
}

export default ContextSelector;
