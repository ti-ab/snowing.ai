import { DataPacket_Kind, Room } from 'livekit-client';

// par exemple, dans un React component :
function ContextSelector({ room }: { room: Room }) {
  const [ctxKey, setCtxKey] = useState<'anglais_60'|'anglais_base'>('anglais_60');

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
      <option value="anglais_60">Leçon d’anglais 60 min</option>
      <option value="anglais_base">Anglais basique</option>
    </select>
  );
}
