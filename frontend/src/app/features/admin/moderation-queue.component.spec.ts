import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ModerationQueueComponent } from './moderation-queue.component';
import { JobPosting } from '../job/job-posting.model';

describe('ModerationQueueComponent', () => {
  let fixture: ComponentFixture<ModerationQueueComponent>;
  let component: ModerationQueueComponent;
  let httpMock: HttpTestingController;

  const postings: JobPosting[] = [
    {
      id: 'job-1',
      title: 'Automotive QA Engineer',
      description: 'desc',
      sector: 'automotive',
      city: 'Tangier',
      contractType: 'CDI',
      status: 'LIVE',
      companyName: 'Atlas Automotive',
    },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ModerationQueueComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    fixture = TestBed.createComponent(ModerationQueueComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loads and displays the moderation queue', () => {
    fixture.detectChanges();
    httpMock.expectOne('/api/v1/admin/postings').flush(postings);

    expect(component.postings()).toEqual(postings);
    expect(component.loading()).toBe(false);
  });

  it('approve removes the posting from the list on success', () => {
    fixture.detectChanges();
    httpMock.expectOne('/api/v1/admin/postings').flush(postings);

    component.approve(postings[0]);
    const req = httpMock.expectOne('/api/v1/admin/postings/job-1/moderate');
    expect(req.request.body).toEqual({ action: 'APPROVE', reason: undefined });
    req.flush({});

    expect(component.postings()).toEqual([]);
  });

  it('reject requires a non-empty reason before submitting', () => {
    fixture.detectChanges();
    httpMock.expectOne('/api/v1/admin/postings').flush(postings);

    component.startReject(postings[0]);
    component.confirmReject(postings[0]);
    httpMock.expectNone('/api/v1/admin/postings/job-1/moderate');

    component.reasonText.set('Misleading title');
    component.confirmReject(postings[0]);
    const req = httpMock.expectOne('/api/v1/admin/postings/job-1/moderate');
    expect(req.request.body).toEqual({ action: 'REJECT', reason: 'Misleading title' });
    req.flush({});

    expect(component.postings()).toEqual([]);
  });

  it('surfaces an error when a moderation action fails', () => {
    fixture.detectChanges();
    httpMock.expectOne('/api/v1/admin/postings').flush(postings);

    component.remove(postings[0]);
    httpMock
      .expectOne('/api/v1/admin/postings/job-1/moderate')
      .flush('conflict', { status: 409, statusText: 'Conflict' });

    expect(component.error()).toBe('Could not apply that moderation action. Please try again.');
    expect(component.postings()).toEqual(postings);
  });
});
