package com.liu.camera;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.VideoView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.security.auth.login.LoginException;

public class MediaActivity extends AppCompatActivity {
    private Button button;
    private Button button1;
    private VideoView mVideoView;
    private String netPath = "http://jdplay.lecloud.com/play.videocache.lecloud.com/187/28/92/letv-gug/14/ver_00_22-1051581402-avc-1507856-aac-96000-117151-23680505-2e0b3774490e51ac469db4313025b877-1466497857703.mp4?crypt=13aa7f2e25900&b=259&nlh=4096&nlt=60&bf=8000&p2p=1&video_type=mp4&termid=0&tss=no&platid=3&splatid=345&its=0&qos=3&fcheck=0&amltag=7&mltag=7&uid=3663232631.rp&keyitem=GOw_33YJAAbXYE-cnQwpfLlv_b2zAkYctFVqe5bsXQpaGNn3T1-vhw..&ntm=1553515800&nkey=f2230cd511223543acdd24e6cec84eff&nkey2=2479b45597488a767015d4251beb7875&auth_key=1553515800-1-0-3-345-37a8f78c20cee9b8cd9c0c9b7f7cebd2&geo=CN-23-323-1&mmsid=65565355&tm=1499247143&key=f0eadb4f30c404d49ff8ebad673d3742&playid=0&vtype=21&cvid=2026135183914&payff=0&sign=mb&dname=mobile&tag=mobile&xformat=super&uidx=0&errc=424&gn=50038&ndtype=2&vrtmcd=102&buss=7&cips=218.88.126.119";
    private String mOutputVideoPath = Environment.getExternalStorageDirectory().getPath()+"/temp.mp4";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_api);
        button = findViewById(R.id.btn);
        button1 = findViewById(R.id.btn1);
        mVideoView = findViewById(R.id.video_v);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    button.setText("合成中……");
                    analysisAudioAndVideo();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("-----------","异常"+e.toString());
                }
            }
        });
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playVideo();
            }
        });
    }
    private void analysisAudioAndVideo() throws IOException{
        int mainAudioMaxInputSize = 0; //能获取的音频的最大值
        int frameMaxInputSize = 0; //能获取的视频的最大值
        int frameRate = 0; //视频的帧率

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {

            MediaMuxer mMediaMuxer = new MediaMuxer(mOutputVideoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            /**
             * 视频的MediaExtractor
             */
            MediaExtractor mVideoExtractor = new MediaExtractor();
            mVideoExtractor.setDataSource(netPath);
            int videoTrackIndex = -1;                                                               //视频轨
            for (int i=0;i<mVideoExtractor.getTrackCount();i++){                                    //循环轨道数，获取需要的视频轨
                MediaFormat mediaFormat = mVideoExtractor.getTrackFormat(i);                        //得到指定索引的记录格式
                if (mediaFormat.getString(MediaFormat.KEY_MIME).startsWith("video/")){              //指定mime类型的媒体格式作为筛选条件  String type = mediaFormat.getString(MediaFormat.KEY_MIME);
                    mVideoExtractor.selectTrack(i);                                                 //将提供视频的视频选择到视轨上
                    videoTrackIndex = mMediaMuxer.addTrack(mediaFormat);                            //将视轨添加到MediaMuxer，并返回新的轨道
                    frameMaxInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);     //得到能获取的有关视频的最大值
                        frameRate = mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE);             //获取视频的帧率
                    Log.e("--------",frameMaxInputSize+"");
                }
            }

            /**
             * 音频的MediaExtractor
             */
            MediaExtractor mAutioExtractor = new MediaExtractor();
            mAutioExtractor.setDataSource(netPath);
            int autioTrackIndex = -1;                                                               //音频轨
            Log.e("视频轨道",mAutioExtractor.getTrackFormat(0)+"");
            Log.e("音频轨道",mAutioExtractor.getTrackFormat(1)+"");
            for (int i=0;i<mAutioExtractor.getTrackCount();i++){                                    //循环轨道数，获取需要的音频轨
                MediaFormat mediaFormat = mAutioExtractor.getTrackFormat(i);                        //得到指定索引的记录格式
                if (mediaFormat.getString(MediaFormat.KEY_MIME).startsWith("audio/")){              //指定mime类型的媒体格式作为筛选条件 找到音轨
                    mAutioExtractor.selectTrack(i);                                                 //将提供音频的视频选择到音轨上
                    autioTrackIndex = mMediaMuxer.addTrack(mediaFormat);                            //将音轨添加到MediaMuxer，并返回新的轨道
                    mainAudioMaxInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE); //得到能获取的有关音频的最大值
                    Log.e("--------",mainAudioMaxInputSize+"");
                }
            }

            //添加完毕所有轨道以后调用start()方法，后面不要忘记释放资源
            mMediaMuxer.start();

            //封装视频track
            if (-1 != videoTrackIndex) {
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                info.presentationTimeUs = 0;
                ByteBuffer buffer = ByteBuffer.allocate(frameMaxInputSize);
                while (true) {
                    int sampleSize = mVideoExtractor.readSampleData(buffer, 0);//检索当前编码的样本并将其存储在字节缓冲区中，读取一帧的视频
                    if (sampleSize < 0) {//如果没有可获取的样本则退出循环
                        break;
                    }
                    //设置样本编码信息
                    info.offset = 0;//必须填入数据的大小
                    info.size = sampleSize;
                    info.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;// 需要给出是否为同步帧/关键帧
                    info.presentationTimeUs += 1000 * 1000 / frameRate;//必须给出正确的时间戳，注意单位是 us
                    mMediaMuxer.writeSampleData(videoTrackIndex, buffer, info);//将样本写入
                    mVideoExtractor.advance(); //推进到下一个样本，类似快进，加载下一帧
                }
            }

            // 封装音频track
            if (-1 != autioTrackIndex) {
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                info.presentationTimeUs = 0;
                ByteBuffer buffer = ByteBuffer.allocate(mainAudioMaxInputSize);
                while (true) {
                    int sampleSize = mAutioExtractor.readSampleData(buffer, 0);
                    if (sampleSize < 0) {
                        break;
                    }
                    info.offset = 0;
                    info.size = sampleSize;
                    info.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
                    info.presentationTimeUs = mAutioExtractor.getSampleTime();
                    mMediaMuxer.writeSampleData(autioTrackIndex, buffer, info);
                    mAutioExtractor.advance();
                }
            }
            // 释放MediaExtractor
            mVideoExtractor.release();
            mAutioExtractor.release();

            // 释放MediaMuxer
            mMediaMuxer.stop();
            mMediaMuxer.release();

            Log.e("---------------","合并完成!");
            button.setText("合并完成！");
        }

    }





    public static String getloToDate(long lo){
        Date date = new Date(lo);
        SimpleDateFormat sd = new SimpleDateFormat("yyyyMMddHHmmss");
        return sd.format(date);
    }


    private void playVideo() {
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mVideoView.start();
            }
        });
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopPlayVideo();
            }
        });
        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                stopPlayVideo();
                return true;
            }
        });

        try {
            Uri uri = Uri.parse(mOutputVideoPath);
            mVideoView.setVideoURI(uri);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void stopPlayVideo() {
        try {
            mVideoView.stopPlayback();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (!mVideoView.isPlaying()) {
            mVideoView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVideoView.canPause()) {
            mVideoView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPlayVideo();
    }
}
