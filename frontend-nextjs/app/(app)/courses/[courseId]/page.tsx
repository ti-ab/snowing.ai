import type {AppConfig} from '@/lib/types';
import CoursePage from "@/components/course-page";
import {getAppConfig} from "@/lib/utils";
import { headers } from 'next/headers';


interface AppProps {
    params: any;
    appConfig: AppConfig;
}

export default async function Page({params}) {

    const hdrs = await headers();
    const appConfig = await getAppConfig(hdrs);


    return <CoursePage courseId={params.courseId} appConfig={appConfig}/>

}
