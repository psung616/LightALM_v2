import { useEffect, useRef, useState } from 'react';

const ACTIVITY_MESSAGES = [
  'DB 조회 중...',
  '데이터 동기화 중...',
  '요청 처리 중...',
  '인덱스 확인 중...',
  '캐시 갱신 중...',
  '세션 검증 중...',
];

const VISIBLE_DURATION_MS = 850;

export function ClickActivityIndicator() {
  const [message, setMessage] = useState(ACTIVITY_MESSAGES[0]);
  const [visible, setVisible] = useState(false);
  const hideTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    function handleClick() {
      setMessage(ACTIVITY_MESSAGES[Math.floor(Math.random() * ACTIVITY_MESSAGES.length)]);
      setVisible(true);
      if (hideTimerRef.current) clearTimeout(hideTimerRef.current);
      hideTimerRef.current = setTimeout(() => setVisible(false), VISIBLE_DURATION_MS);
    }

    window.addEventListener('click', handleClick);
    return () => {
      window.removeEventListener('click', handleClick);
      if (hideTimerRef.current) clearTimeout(hideTimerRef.current);
    };
  }, []);

  return (
    <div
      className={`pointer-events-none fixed bottom-5 right-5 z-[999] flex items-center gap-2 rounded-full border border-[#23252a] bg-[#141516] px-3 py-2 shadow-lg transition-all duration-200 ${
        visible ? 'translate-y-0 opacity-100' : 'translate-y-2 opacity-0'
      }`}
    >
      <span className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-[#34343a] border-t-[#5e6ad2]" />
      <span className="text-xs font-medium text-[#d0d6e0]">{message}</span>
    </div>
  );
}
