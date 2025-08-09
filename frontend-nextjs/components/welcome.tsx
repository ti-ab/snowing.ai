import { Button } from '@/components/ui/button';

interface WelcomeProps {
  disabled: boolean;
  startButtonText: string;
  onStartCall: () => void;
}

export const Welcome = ({
                          disabled,
                          startButtonText,
                          onStartCall,
                          books,
                          ref,
                          router
                        }: React.ComponentProps<'div'> & WelcomeProps) => {

  return (
    <div
      ref={ref}
      inert={disabled}
      className="z-10 mx-auto h-screen flex flex-col items-center"
    >
      {!!books?.length ? <div className={"mt-24"}>
        {books.map((book: any, index: number) => <div key={index}>
          <h2 className={"cursor-pointer text-blue-600 underline"} onClick={() => router.push(`/courses/${book.id}`)}><b>{index+1}. {book.title}</b></h2> ({book.chapters?.length} chapters)
        </div>)}
      </div> : <div>Loading courses...</div>}
    </div>
  );
};
